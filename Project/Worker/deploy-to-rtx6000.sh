#!/bin/bash
# ============================================================
# RTX 6000 연구실 서버 GPU Worker 배포 스크립트
# 실행: bash Project/Worker/deploy-to-rtx6000.sh
# ============================================================

set -e

RTX6000_USER="sslab"
RTX6000_HOST="155.230.118.52"
RTX6000_PORT="22345"
RTX6000_PASS="sslab1!2"
REMOTE_DIR="/home/sslab/Documents/gpu-worker"
WORKER_PORT="5001"

echo "============================================================"
echo "  RTX 6000 GPU Worker 배포 시작"
echo "  대상 서버: ${RTX6000_USER}@${RTX6000_HOST}:${RTX6000_PORT}"
echo "============================================================"

# sshpass 설치 확인 (비밀번호 자동 입력용)
if ! command -v sshpass &> /dev/null; then
    echo "[!] sshpass가 설치되지 않았습니다."
    echo "    Ubuntu/Debian: sudo apt install sshpass"
    echo "    macOS:         brew install hudochenkov/sshpass/sshpass"
    echo ""
    echo "    sshpass 없이 진행하려면 아래 명령어를 수동으로 실행하세요:"
    echo "    scp -P ${RTX6000_PORT} Project/Worker/gpu_worker.py ${RTX6000_USER}@${RTX6000_HOST}:${REMOTE_DIR}/"
    exit 1
fi

SSH_CMD="sshpass -p '${RTX6000_PASS}' ssh -p ${RTX6000_PORT} -o StrictHostKeyChecking=no ${RTX6000_USER}@${RTX6000_HOST}"
SCP_CMD="sshpass -p '${RTX6000_PASS}' scp -P ${RTX6000_PORT} -o StrictHostKeyChecking=no"

echo ""
echo "[1/4] 원격 디렉터리 생성..."
eval "${SSH_CMD} 'mkdir -p ${REMOTE_DIR}'"
echo "  ✅ 완료"

echo ""
echo "[2/4] Worker 파일 업로드..."
eval "${SCP_CMD} Project/Worker/gpu_worker.py ${RTX6000_USER}@${RTX6000_HOST}:${REMOTE_DIR}/"
eval "${SCP_CMD} Project/Worker/requirements-gpu.txt ${RTX6000_USER}@${RTX6000_HOST}:${REMOTE_DIR}/"
echo "  ✅ 완료"

echo ""
echo "[3/4] Python 패키지 설치..."
eval "${SSH_CMD} 'cd ${REMOTE_DIR} && pip install -q -r requirements-gpu.txt'"
echo "  ✅ 완료"

echo ""
echo "[4/4] 기존 Worker 종료 후 새로 실행 (tmux 세션: gpu-worker)..."
# 기존 tmux 세션 종료 (있으면)
eval "${SSH_CMD} 'tmux kill-session -t gpu-worker 2>/dev/null || true'"
# 새 tmux 세션으로 Worker 시작
eval "${SSH_CMD} 'tmux new-session -d -s gpu-worker \"cd ${REMOTE_DIR} && uvicorn gpu_worker:app --host 0.0.0.0 --port ${WORKER_PORT} 2>&1 | tee gpu_worker.log\"'"
sleep 3

echo "  ✅ 완료"
echo ""
echo "============================================================"
echo "  배포 완료!"
echo "  Worker API: http://${RTX6000_HOST}:${WORKER_PORT}"
echo ""
echo "  헬스체크 확인:"
echo "  curl http://${RTX6000_HOST}:${WORKER_PORT}/health"
echo ""
echo "  로그 확인 (서버에서):"
echo "  ssh -p ${RTX6000_PORT} ${RTX6000_USER}@${RTX6000_HOST}"
echo "  tmux attach -t gpu-worker"
echo "============================================================"

# 헬스체크 자동 실행
echo ""
echo "[헬스체크] Worker 상태 확인 중..."
sleep 2
curl -s "http://${RTX6000_HOST}:${WORKER_PORT}/health" | python3 -m json.tool 2>/dev/null || \
    echo "[!] 아직 응답 없음 — 방화벽 설정 또는 포트 개방이 필요할 수 있습니다."
