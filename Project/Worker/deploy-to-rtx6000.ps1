# ============================================================
# RTX 6000 연구실 서버 GPU Worker 배포 스크립트 (Windows PowerShell용)
# 실행: .\Project\Worker\deploy-to-rtx6000.ps1
# ============================================================

$RTX6000_USER = "sslab"
$RTX6000_HOST = "155.230.118.52"
$RTX6000_PORT = "22345"
$REMOTE_DIR   = "/home/sslab/Documents/gpu-workspace/gpu-worker"
$WORKER_PORT  = "5001"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition
$WORKER_PY  = Join-Path $SCRIPT_DIR "gpu_worker.py"
$REQS_TXT   = Join-Path $SCRIPT_DIR "requirements-gpu.txt"

Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  RTX 6000 GPU Worker 배포 (Windows PowerShell)" -ForegroundColor Magenta
Write-Host "  대상: ${RTX6000_USER}@${RTX6000_HOST}:${RTX6000_PORT}" -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "  비밀번호 입력 안내:" -ForegroundColor Yellow
Write-Host "  각 단계마다 비밀번호를 물어보면 [ sslab1!2 ] 를 입력하세요." -ForegroundColor Yellow
Write-Host ""

# ssh / scp 설치 확인
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    Write-Host "[오류] ssh 명령어를 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "  → Windows 설정 > 앱 > 선택적 기능 > 'OpenSSH 클라이언트' 설치 후 재시도하세요." -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/4] 원격 디렉터리 생성 중..." -ForegroundColor Cyan
ssh -p $RTX6000_PORT -o StrictHostKeyChecking=no "${RTX6000_USER}@${RTX6000_HOST}" "mkdir -p $REMOTE_DIR"
Write-Host "  ✅ 완료" -ForegroundColor Green

Write-Host ""
Write-Host "[2/4] gpu_worker.py 업로드 중..." -ForegroundColor Cyan
scp -P $RTX6000_PORT -o StrictHostKeyChecking=no $WORKER_PY "${RTX6000_USER}@${RTX6000_HOST}:${REMOTE_DIR}/"
Write-Host "  ✅ 완료" -ForegroundColor Green

Write-Host ""
Write-Host "[3/4] requirements-gpu.txt 업로드 중..." -ForegroundColor Cyan
scp -P $RTX6000_PORT -o StrictHostKeyChecking=no $REQS_TXT "${RTX6000_USER}@${RTX6000_HOST}:${REMOTE_DIR}/"
Write-Host "  ✅ 완료" -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] 서버에서 패키지 설치 및 Worker 시작 중..." -ForegroundColor Cyan
Write-Host "  (비밀번호를 한 번 더 입력하세요)" -ForegroundColor Yellow
ssh -p $RTX6000_PORT -o StrictHostKeyChecking=no "${RTX6000_USER}@${RTX6000_HOST}" @"
cd $REMOTE_DIR
echo '[서버] 패키지 설치 중...'
pip install -q fastapi uvicorn
echo '[서버] 기존 Worker 종료...'
pkill -f 'uvicorn gpu_worker' 2>/dev/null || true
sleep 1
echo '[서버] Worker 시작 (백그라운드)...'
nohup uvicorn gpu_worker:app --host 0.0.0.0 --port $WORKER_PORT > gpu_worker.log 2>&1 &
sleep 2
echo '[서버] Worker 상태:'
curl -s http://localhost:$WORKER_PORT/health || echo '아직 시작 중...'
echo ''
echo '[완료] Worker가 포트 $WORKER_PORT 에서 실행 중입니다.'
"@

Write-Host ""
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  배포 완료!" -ForegroundColor Green
Write-Host ""
Write-Host "  Worker API: http://${RTX6000_HOST}:${WORKER_PORT}" -ForegroundColor White
Write-Host ""
Write-Host "  헬스체크 (아래 명령 실행):" -ForegroundColor Cyan
Write-Host "  Invoke-RestMethod http://${RTX6000_HOST}:${WORKER_PORT}/health" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Magenta
