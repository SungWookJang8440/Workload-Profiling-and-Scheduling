"""
GPU Worker — RTX 6000 연구실 서버용 경량 FastAPI 서버
=====================================================
- POST /run      : GPU 연산 백그라운드 실행 (torch.matmul)
- GET  /metrics  : nvidia-smi 실시간 수치 반환
- GET  /status   : 현재 실행 중인 작업 상태
- POST /stop     : 실행 중인 작업 강제 종료

실행 방법 (RTX 6000 서버에서):
    pip install fastapi uvicorn
    uvicorn gpu_worker:app --host 0.0.0.0 --port 5001
"""

import subprocess
import threading
import time
import os
import signal
from typing import Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="GPU Worker API", version="1.0.0")

# CORS 허용 (백엔드 Spring Boot에서 호출)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─────────────────────────────────────────────
# 전역 상태
# ─────────────────────────────────────────────
class JobState:
    def __init__(self):
        self.job_id: Optional[str] = None
        self.workload_name: Optional[str] = None
        self.status: str = "IDLE"   # IDLE | RUNNING | DONE | ERROR
        self.started_at: Optional[float] = None
        self.duration_sec: int = 0
        self.pid: Optional[int] = None
        self._lock = threading.Lock()

    def set_running(self, job_id: str, workload_name: str, duration_sec: int, pid: int):
        with self._lock:
            self.job_id = job_id
            self.workload_name = workload_name
            self.status = "RUNNING"
            self.started_at = time.time()
            self.duration_sec = duration_sec
            self.pid = pid

    def set_done(self):
        with self._lock:
            self.status = "DONE"
            self.pid = None

    def set_idle(self):
        with self._lock:
            self.job_id = None
            self.workload_name = None
            self.status = "IDLE"
            self.started_at = None
            self.duration_sec = 0
            self.pid = None

    def to_dict(self):
        with self._lock:
            elapsed = round(time.time() - self.started_at, 1) if self.started_at else 0
            return {
                "job_id": self.job_id,
                "workload_name": self.workload_name,
                "status": self.status,
                "elapsed_sec": elapsed,
                "duration_sec": self.duration_sec,
                "remaining_sec": max(0, self.duration_sec - elapsed),
            }

job_state = JobState()


# ─────────────────────────────────────────────
# 요청 모델
# ─────────────────────────────────────────────
class RunRequest(BaseModel):
    job_id: str
    workload_name: str
    duration_sec: int = 30      # 연산 지속 시간 (초)
    intensity: int = 4096       # 행렬 크기 (클수록 GPU 부하 ↑)


# ─────────────────────────────────────────────
# GPU 연산 실행 함수 (별도 프로세스)
# ─────────────────────────────────────────────
GPU_SCRIPT_TEMPLATE = """
import torch
import time
import sys

duration = {duration}
size = {size}

if not torch.cuda.is_available():
    print("ERROR: CUDA not available", flush=True)
    sys.exit(1)

device = torch.device('cuda')
print(f"GPU: {{torch.cuda.get_device_name(0)}}", flush=True)
print(f"Starting workload: {workload_name}", flush=True)

# 4GB VRAM 강제 할당 (nvidia-smi 메모리 변화 가시화)
# 1024 * 1024 * 1024 floats * 4 bytes = 4 GB
mem_placeholder = torch.zeros(1024 * 1024 * 1024, dtype=torch.float32, device=device)

x = torch.randn(size, size, device=device)
y = torch.randn(size, size, device=device)

start = time.time()
last_print = start
while time.time() - start < duration:
    # 0.1초 동안 쉬지 않고 행렬 곱 수행하여 SM 사용률(util) 80% 이상 강제 유도
    loop_start = time.time()
    while time.time() - loop_start < 0.1:
        z = torch.matmul(x, y)
    torch.cuda.synchronize()
    time.sleep(0.02) # 짧은 휴식으로 오버히트 방지 및 타 업무 양보
    
    now = time.time()
    if now - last_print >= 1.0:
        elapsed = round(now - start, 1)
        print(f"Progress: {{elapsed}}s / {duration}s", flush=True)
        last_print = now

# 자원 해제
del mem_placeholder
del x
del y
torch.cuda.empty_cache()
print("Workload complete.", flush=True)
"""


def _run_gpu_job(req: RunRequest):
    """백그라운드 스레드에서 GPU Python 프로세스 실행"""
    script = GPU_SCRIPT_TEMPLATE.format(
        duration=req.duration_sec,
        size=req.intensity,
        workload_name=req.workload_name,
    )

    try:
        proc = subprocess.Popen(
            ["python3", "-c", script],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
        job_state.set_running(req.job_id, req.workload_name, req.duration_sec, proc.pid)

        # 프로세스 완료 대기 및 로그 출력
        for line in proc.stdout:
            print(f"[GPU Worker] {line.strip()}")

        proc.wait()
        job_state.set_done()
        time.sleep(3)       # DONE 상태를 잠시 유지
        job_state.set_idle()

    except Exception as e:
        print(f"[GPU Worker] ERROR: {e}")
        job_state.set_idle()


# ─────────────────────────────────────────────
# API 엔드포인트
# ─────────────────────────────────────────────

@app.get("/health")
def health():
    """헬스체크 — 백엔드에서 Worker 활성 여부 확인용"""
    cuda_ok = False
    try:
        result = subprocess.run(
            ["nvidia-smi", "--query-gpu=name", "--format=csv,noheader"],
            capture_output=True, text=True, timeout=5
        )
        cuda_ok = result.returncode == 0
    except Exception:
        pass

    return {
        "status": "ok",
        "gpu_available": cuda_ok,
        "current_job": job_state.to_dict(),
    }


@app.post("/run")
def run_job(req: RunRequest):
    """GPU 연산 실행 요청"""
    current = job_state.to_dict()
    if current["status"] == "RUNNING":
        raise HTTPException(
            status_code=409,
            detail=f"이미 실행 중인 작업이 있습니다: {current['workload_name']} (job_id: {current['job_id']})"
        )

    # 백그라운드 스레드로 실행
    thread = threading.Thread(target=_run_gpu_job, args=(req,), daemon=True)
    thread.start()

    return {
        "status": "accepted",
        "job_id": req.job_id,
        "workload_name": req.workload_name,
        "duration_sec": req.duration_sec,
        "message": f"RTX 6000에서 '{req.workload_name}' 작업을 시작합니다.",
    }


@app.get("/metrics")
def get_metrics():
    """nvidia-smi로 실시간 GPU 사용률 반환"""
    try:
        result = subprocess.run(
            [
                "nvidia-smi",
                "--query-gpu=utilization.gpu,utilization.memory,memory.used,memory.total,power.draw,temperature.gpu",
                "--format=csv,noheader,nounits",
            ],
            capture_output=True,
            text=True,
            timeout=5,
        )
        if result.returncode != 0:
            raise RuntimeError(result.stderr)

        vals = [v.strip() for v in result.stdout.strip().split(",")]
        return {
            "sm_util":    float(vals[0]),       # SM 사용률 (%)
            "mem_util":   float(vals[1]),       # 메모리 사용률 (%)
            "mem_used_mb": float(vals[2]),      # 사용 중인 메모리 (MB)
            "mem_total_mb": float(vals[3]),     # 전체 메모리 (MB)
            "power_w":    float(vals[4]),       # 전력 소비 (W)
            "temp_c":     float(vals[5]),       # 온도 (°C)
            "job": job_state.to_dict(),
        }
    except FileNotFoundError:
        raise HTTPException(status_code=500, detail="nvidia-smi를 찾을 수 없습니다. NVIDIA 드라이버가 설치되어 있는지 확인하세요.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"메트릭 수집 실패: {str(e)}")


@app.get("/status")
def get_status():
    """현재 실행 중인 작업 상태 조회"""
    return job_state.to_dict()


@app.post("/stop")
def stop_job():
    """실행 중인 작업 강제 종료"""
    current = job_state.to_dict()
    if current["status"] != "RUNNING":
        return {"status": "no_running_job", "message": "실행 중인 작업이 없습니다."}

    pid = job_state.pid
    if pid:
        try:
            os.kill(pid, signal.SIGTERM)
            time.sleep(1)
            os.kill(pid, signal.SIGKILL)
        except ProcessLookupError:
            pass  # 이미 종료됨

    job_state.set_idle()
    return {"status": "stopped", "message": f"작업 (PID {pid})을 종료했습니다."}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5001)
