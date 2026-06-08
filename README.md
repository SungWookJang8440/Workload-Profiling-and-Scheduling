# Workload Profiling & GPU Scheduling (GPU Sharing Project)

GPU 공유 인프라 환경에서 워크로드를 자동으로 프로파일링하고, **다중 기준 의사결정(MCDM) 알고리즘**과 **동적 부하 분산(Dynamic Load Balancing)**을 통해 최적의 GPU 노드에 작업을 스케줄링하는 연구 및 구현 프로젝트입니다.

---

## ✨ 핵심 기능

| 기능 | 설명 |
|------|------|
| 🧠 **MCDM 스케줄링** | 성능(Perf), 자원 적합도(Fit), 비용효율(Cost), 전력효율(Power) 4개 기준으로 최적 GPU 선택 |
| ⚡ **동적 우회 할당** | 대기열(TTE) 분석 후 MCDM 1순위가 혼잡하면 실시간으로 더 빠른 노드로 Bypass Routing |
| 🌐 **자연어 프롬프트** | "BERT 모델 배치 8로 학습해줘" 같은 자연어를 Gemini API로 파싱해 작업(w0~w17) 자동 매핑 |
| 📊 **실시간 Gantt 차트** | RTX 3090 / 4090 / 6000 노드별 대기열 타임라인 실시간 렌더링 |
| 📝 **라이브 로그 터미널** | 스케줄링 결정 이유, Bypass 발생, 작업 완료까지 한글로 실시간 출력 |

---

## 📂 프로젝트 구조

```
Workload-Profiling-and-Scheduling/
├── Project/
│   ├── Backend/         # Spring Boot (Java 21, Maven) API 서버
│   ├── Frontend/        # React + Vite + TypeScript 대시보드
│   ├── Infra/           # Docker Compose, 배포 스크립트
│   ├── Algorithm/       # MCDM 알고리즘 연구 (Python)
│   └── Worker/          # GPU 워커 에이전트
├── Archive/             # 이전 버전 백업
├── start.ps1            # ⭐ 원클릭 실행 스크립트 (로컬 개발용)
└── README.md
```

---

## 🚀 빠른 시작

### 방법 1: Docker (권장 — 노트북/팀원 환경)

> Docker Desktop이 설치되어 있어야 합니다.

```bash
# 1. 저장소 클론
git clone https://github.com/SungWookJang8440/Workload-Profiling-and-Scheduling.git
cd Workload-Profiling-and-Scheduling

# 2. Infra 폴더로 이동 후 실행
cd Project/Infra
docker-compose up --build -d

# 3. 접속
# 프론트엔드: http://localhost:4000
# 백엔드 헬스체크: http://localhost:8000/health
```

**처음 빌드 시 5~10분 소요** (Maven 의존성 + npm 패키지 다운로드)

#### Docker 주요 명령어
```bash
docker-compose ps                    # 실행 상태 확인
docker-compose logs -f backend       # 백엔드 로그
docker-compose logs -f frontend      # 프론트엔드 로그
docker-compose down                  # 중지
docker-compose down -v               # 완전 초기화 (DB 포함 삭제)
docker-compose up --build -d         # 코드 변경 후 재빌드
```

---

### 방법 2: 로컬 직접 실행 (Windows PowerShell)

> Java 21, Node.js, PostgreSQL이 로컬에 설치되어 있어야 합니다. (Maven은 스크립트 실행 시 존재하지 않으면 자동으로 다운로드 및 설정됩니다.)

```powershell
# 1. 서버 구동 (백엔드와 프론트엔드가 각각의 새 PowerShell 창에서 기동됩니다)
.\start.ps1

# 2. 서버 종료 (포트 8000, 5173을 사용하는 프로세스를 안전하게 강제 종료합니다)
.\start.ps1 -Stop
```

서버 기동 완료 후 접속:
- **대시보드 / 스케줄러**: http://localhost:5173
- **백엔드 API 및 헬스체크**: http://localhost:8000/health

---

## 🖥️ MCDM 스케줄러 대시보드 사용법

### 접속 경로
```
http://localhost:4000/scheduler   # Docker 환경
http://localhost:5173/scheduler  # 로컬 개발 환경
```

### 시연 시나리오 (RTX 6000 → 4090 → 3090 선택 확인)

#### 1. RTX 6000이 선택되는 경우 (초기 상태)
```
① [초기화] 버튼 클릭
② 프롬프트 입력: "BERT 모델 파인튜닝 배치 8로 돌려줘"
③ [분석 및 스케줄링 실행] 클릭
→ RTX 6000 선택됨 (대기 시간 0초 → ETA 최소)
```

#### 2. RTX 4090이 선택되는 경우 (RTX 6000 대기열 누적 시)
```
① [초기화] 클릭 후 [자동] 버튼 켜기
② 약 30초 대기 → RTX 4090 대기열(30초)이 0초로 소모됨
③ [일시정지] 후 프롬프트 입력: "ResNet-50 트레이닝 배치 32로 진행해줘"
④ [분석 및 스케줄링 실행] 클릭
→ RTX 4090 선택됨 (RTX 6000보다 ETA 짧음)
```

#### 3. RTX 3090이 선택되는 경우 (모든 대기열 소모 후 특정 워크로드)
```
① [초기화] 클릭 후 [자동] 버튼 켜기
② 약 120초 대기 → 전체 GPU 대기열이 0초로 소모됨
③ [일시정지] 후 프롬프트 입력: "ResNet-50 트레이닝 배치 64로 진행해줘"
④ [분석 및 스케줄링 실행] 클릭
→ RTX 3090 선택됨 (해당 워크로드는 3090이 실행시간 더 짧음)
```

### 추천 자연어 프롬프트
| 입력 예시 | 매핑 워크로드 |
|----------|-------------|
| `BERT 모델 파인튜닝 배치 8로 돌려줘` | bert-base-cased-train (batch8) |
| `ResNet-50 트레이닝 배치 32로 진행해줘` | resnet50-train (batch32) |
| `Whisper 라지 모델로 음성 인식해줘 배치 4` | openai-whisper-large-v2-inf (batch4) |
| `MobileNet 추론 배치 32로 실행해줘` | google-mobilenet_v2-inf (batch32) |
| `ViT 이미지 분류 배치 16으로 추론해줘` | google-vit-base-patch16-224-inf (batch16) |

---

## 🔗 실제 RTX 6000 GPU 노드 연동 (SSH 터널링)

본 프로젝트는 로컬 개발 환경에서 실제 연구실 서버의 **RTX 6000 GPU 노드**에 원격으로 연산을 요청하고 실시간 하드웨어 지표(SM%, VRAM)를 수집할 수 있습니다. 
보안 문제로 GPU 에이전트 포트(5001)가 외부에 열려있지 않으므로, **SSH 터널링(로컬 포트 포워딩)**을 사용해 통신을 연결합니다.

### 1. SSH 터널링 세션 연결 (로컬 터미널)
로컬 서버들을 켜기 전, 개별 터미널 창을 열어 아래 명령어를 통해 SSH 연결을 맺고 세션을 유지합니다.
```bash
ssh -p 22345 -L 5001:localhost:5001 sslab@155.230.118.52
```
*※ 시연 및 모니터링이 완료될 때까지 이 터미널 창은 유지되어야 합니다.*

### 2. 백엔드 설정 확인
- 백엔드 [GpuNodeRegistry.java](file:///Project/Backend/src/main/java/com/gpu/sharing/scheduler/GpuNodeRegistry.java)의 `g2` 노드 주소는 `http://localhost:5001`로 이미 설정되어 있습니다. 로컬 포트 5001로 나가는 모든 요청은 SSH 터널을 타고 원격 RTX 6000의 GPU 에이전트로 중계됩니다.

### 3. 실제 GPU 로드 생성 및 모니터링 테스트 방법
1. 로컬 환경에서 `.\start.ps1`을 실행하여 전체 시스템을 켭니다.
2. `http://localhost:5173/scheduler`로 접속합니다.
3. 입력창에 **"BERT 모델 파인튜닝 배치 8로 돌려줘"** 프롬프트를 입력하고 스케줄링 실행합니다.
4. MCDM 알고리즘에 의해 RTX 6000(`g2`)이 최적 노드로 매핑되며, 실제 RTX 6000에 연산(4GB VRAM 점유 및 80% 이상의 SM 사용량)이 로드됩니다.
5. 대시보드 화면상에서 RTX 6000 카드의 SM 사용률과 메모리 잔량 수치가 실시간(2초 폴링)으로 치솟았다가 연산 종료 후 회복되는 것을 직접 시각적으로 확인합니다.

---

## 🔧 환경 변수 설정

백엔드 실행 시 아래 환경변수가 필요합니다. `Project/Backend/.env.example`을 복사하여 `.env`로 만드세요.

```env
SERVER_PORT=8000
DATABASE_URL=jdbc:postgresql://localhost:5432/gpu_sharing
DATABASE_USER=gpu_user
DATABASE_PW=gpu_password
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRE_HOURS=24
GEMINI_API_KEY=your-gemini-api-key   # 자연어 파싱용 (없으면 키워드 fallback 사용)
```

---

## 🤝 협업 규칙

- **브랜치 전략**: `main` (배포) ← `dev` (통합) ← `feat/기능명` (개발)
- **커밋 메시지**: `feat:`, `fix:`, `docs:`, `refactor:` 접두사 사용
- **환경 변수**: `.env` 파일은 절대 커밋하지 않음 (`.env.example`만 제공)

---

## 📚 추가 문서

- [인프라 셋업 가이드](Project/Infra/START_GUIDE.md)
- [백엔드 상세 가이드](Project/Backend/README.md)
- [프론트엔드 상세 가이드](Project/Frontend/README.md)
- [MCDM 알고리즘 설명](Project/Algorithm/)
