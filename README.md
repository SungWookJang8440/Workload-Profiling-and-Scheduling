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

> Java 21, Maven, Node.js, PostgreSQL이 로컬에 설치되어 있어야 합니다.

```powershell
# 프로젝트 루트에서 원클릭 실행
.\start.ps1
```

서버 기동 완료 후 접속:
- **프론트엔드**: http://localhost:5173
- **백엔드**: http://localhost:8000

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
