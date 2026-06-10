# GPU Sharing Project Connection Manual (연결 및 가동 매뉴얼)

이 문서는 GPU Sharing 프로젝트의 각 컴포넌트(백엔드, 프론트엔드, 워커)를 로컬 환경에서 구동하고, 외부 물리 GPU 노드(연구실 RTX 6000 및 vast.ai RTX 4090)를 SSH 및 API를 통해 연동하는 방법을 터미널 명령어를 기반으로 설명합니다.

---

## 1. 로컬 개발 환경 가동 (Docker Compose)

프로젝트 루트의 `Project/Infra` 디렉터리로 이동한 후 Docker Compose를 이용해 모든 컨테이너(백엔드, 프론트엔드, DB, 로컬 워커)를 한 번에 빌드하고 가동합니다.

```powershell
# 1. 인프라 디렉터리로 이동
cd c:\Users\장성욱\Desktop\ws\GPU-sharing-Jang\Project\Infra

# 2. 컨테이너 빌드 및 백그라운드 기동
docker compose up --build -d
```

### 가동 확인 명령어
```powershell
# 컨테이너 가동 및 헬스체크 상태 확인
docker compose ps

# 백엔드 실시간 로그 확인
docker compose logs -f backend
```

---

## 2. 연구실 GPU 노드 연동 및 실시간 사용량 표시 (RTX 6000)

백엔드 및 대시보드 웹상에서 연구실 RTX 6000 GPU의 실시간 자원 정보를 수집하고 표시하기 위한 차례대로의 절차입니다. 

### Step 2.1: 원격 연구실 서버에 GPU Worker 설치 및 기동 (최초 1회 또는 재부팅 시)
원격 연구실 서버 안에서 GPU 정보를 실시간 수집 및 가공하는 FastAPI Worker 애플리케이션이 구동 중이어야 합니다. 

로컬 터미널(PowerShell)을 열어 아래 배포 스크립트를 실행합니다:
```powershell
# 1. 프로젝트 루트 폴더로 이동
cd c:\Users\장성욱\Desktop\ws\GPU-sharing-Jang

# 2. PowerShell 배포 스크립트 실행 (연구실 서버에 Worker 원격 전송 및 nohup 실행)
.\Project\Worker\deploy-to-rtx6000.ps1
```
> 스크립트 진행 중 패스워드 입력 요구 시, 연구실 계정(sslab) 비밀번호를 입력합니다.
> 이 스크립트가 성공하면 연구실 서버 내에 `uvicorn gpu_worker:app --port 5001` 백그라운드 프로세스가 실행됩니다.

### Step 2.2: 로컬 PC에서 SSH 터널링(포트 포워딩) 실행
백엔드 컨테이너는 로컬 PC의 `5001`번 포트를 통해 연구실 Worker API에 접근하도록 기본 설정되어 있습니다. 
새로운 로컬 터미널 창을 열고 아래 터널링 명령을 입력하여 연결을 활성화합니다.

```powershell
# 연구실 서버 내부 5001번 포트(Worker)를 로컬 PC의 5001번 포트로 포워딩
ssh -L 5001:localhost:5001 sslab@155.230.118.52 -p 22345 -N
```
> **중요**: 비밀번호 입력 후 터미널 창을 닫지 않고 **그대로 켜두어야** 터널링 세션이 유지되며 실시간 사용량이 표시됩니다.

### Step 2.3: 백엔드 환경변수 설정 확인 (`Project/Infra/docker-compose.yaml`)
백엔드가 컨테이너 내부에서 로컬 호스트의 5001 포트(포워딩된 연구실 Worker)에 연결하기 위한 설정이 다음과 같이 지정되어 있습니다:
- `RTX6000_WORKER_URL=http://host.docker.internal:5001`
- `RTX6000_SSH_HOST=155.230.118.52`
- `RTX6000_SSH_PORT=22345`
- `RTX6000_SSH_USER=sslab`
- `RTX6000_SSH_PASSWORD=sslab1!2` (자동 모니터링 시 필요)

---

## 3. vast.ai GPU 노드 연동 (RTX 4090)

vast.ai 인스턴스는 시작/재시작할 때마다 **포트 번호가 동적으로 변합니다.** (예: `22099`)

### 3.1. 로컬 SSH 키 기반 접속 설정 및 키 등록
vast.ai 인스턴스는 패스워드가 아닌 SSH Key 기반 접속을 주로 요구합니다. 
로컬 PC의 공개키(`id_ed25519.pub`)를 vast.ai 인스턴스에 먼저 등록해야 합니다.

1. **로컬 PC 공개키 내용 복사**
   ```powershell
   Get-Content "$env:USERPROFILE\.ssh\id_ed25519.pub"
   # 출력 내용(예: ssh-ed25519 AAAAC3Nz... csu8440@gmail.com)을 복사하여 vast.ai 콘솔의 SSH Key에 등록합니다.
   ```

2. **호스트 키 신뢰 목록 추가 및 접속 테스트**
   ```powershell
   # 포트가 22099인 경우의 SSH 직접 연결 및 자원 쿼리 검증 명령어
   ssh -p 22099 root@194.14.47.19 -o StrictHostKeyChecking=no "nvidia-smi"
   ```

### 3.2. 포트 포워딩 (SSH 터널링)을 통한 Worker API 연동
vast.ai 인스턴스 내부에서 돌고 있는 GPU Worker API(5000번 포트)를 로컬 PC의 `5002` 포트로 포워딩하여 백엔드가 메트릭을 수집하도록 연결합니다.

*터미널에서 다음 명령어를 실행하여 터널링을 유지합니다:*
```powershell
# 194.14.47.19의 내부 5000번 포트를 로컬의 5002 포트로 포워딩 (vast.ai 포트가 22099인 경우)
ssh -L 5002:localhost:5000 -p 22099 root@194.14.47.19 -N
```
> `-N` 옵션은 쉘 실행 없이 터널링 포트만 유지시키는 옵션입니다. 터미널을 열어둔 상태로 유지해야 포워딩이 끊기지 않습니다.

### 3.3. 백엔드 설정값 업데이트
vast.ai 인스턴스의 포트가 변경되었을 때, `Project/Infra/docker-compose.yaml` 내 backend 서비스 설정을 변경해 주어야 합니다.

```yaml
      - RTX4090_WORKER_URL=http://host.docker.internal:5002
      - RTX4090_SSH_HOST=194.14.47.19
      - RTX4090_SSH_PORT=22099  # 새로 할당된 포트로 변경
      - RTX4090_SSH_USER=root
```
설정을 변경한 후에는 백엔드 서비스를 재기동해야 변경 사항이 적용됩니다:
```powershell
docker compose up -d backend
```

---

## 4. 연동 결과 검증 흐름

전체 인프라가 기동된 후 정상 연동되었는지 확인하는 파이프라인 흐름입니다.

### 1) 백엔드 헬스 체크 API
```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8000/health"
```

### 2) 스케줄러 분석 API 호출 (자연어 분석)
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8000/scheduler/analyze" -ContentType "application/json" -Body '{"prompt":"ResNet50 모델 학습하고 싶어. 배치 크기는 128로 설정해줘."}'
```
*결과값으로 최적 GPU 추천 정보(RTX 3090 또는 6000 등)와 예상 대기/연산 시간이 JSON으로 반환되는지 확인합니다.*

### 3) 스케줄러 작업 실행 및 할당 API 호출
```powershell
# 추천 결과에 포함된 workloadId(예: w0) 및 gpuId(예: g2)를 본문에 실어 전송
Invoke-RestMethod -Method Post -Uri "http://localhost:8000/scheduler/execute" -ContentType "application/json" -Body '{"workloadId":"w0","gpuId":"g2"}'
```
*이후 프론트엔드 대시보드(http://localhost:4000/dashboard) Gantt 차트에 작업이 할당되고 실시간 메트릭이 변동되는지 확인합니다.*
