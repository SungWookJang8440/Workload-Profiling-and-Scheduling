# GPU Sharing 프로젝트 - 처음부터 시작하기

> Docker + WSL 환경에서 GPU Sharing 백엔드와 프론트엔드를 처음부터 실행하는 가이드

---

## 1. 사전 준비사항

### 필수 설치
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows용)
- [WSL 2 + Ubuntu](https://docs.microsoft.com/ko-kr/windows/wsl/install) (Windows 터미널)

### 확인 명령어
```powershell
# WSL 상태 확인
wsl --status

# Docker 실행 확인
wsl -d Ubuntu docker --version
```

---

## 2. 프로젝트 위치

```text
Project/
├── Infra/            # 인프라 설정 및 도커 파일 (현재 위치)
│   └── docker-compose.yaml
├── Backend/          # Spring Boot 백엔드
└── Frontend/         # React 프론트엔드
```

---

## 3. 처음부터 실행하기 (단계별)

### Step 1: Docker Desktop 실행
Docker Desktop 앱을 먼저 켜세요.

### Step 2: 터미널 열기 및 폴더 이동
```powershell
# 프로젝트의 Infra 폴더로 이동 (경로는 본인 환경에 맞게 수정)
cd /mnt/c/Users/User/Desktop/종프2/Workload-Profiling-and-Scheduling/Project/Infra

# 또는 Windows PowerShell 사용 시:
# cd C:\Users\User\Desktop\종프2\Workload-Profiling-and-Scheduling\Project\Infra
```

### Step 3: 기존 컨테이너 정리 (처음이거나 완전 초기화 시)
```bash
# 모든 컨테이너 중지 및 삭제
docker-compose down -v

# 네트워크 정리
docker network prune -f

# 확인
docker ps -a  # 아무것도 안 떠야 함
```

### Step 4: 빌드 및 실행
```bash
# 백엔드 + 프론트엔드 함께 빌드하고 실행
docker-compose up --build -d
```

**빌드 시간**: 처음에는 5~10분 소요 (Maven 의존성 + npm 패키지 다운로드)

### Step 5: 실행 확인
```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그 확인 (백엔드)
docker-compose logs -f backend

# 로그 확인 (프론트엔드)
docker-compose logs -f frontend
```

---

## 4. 접속 및 테스트

### 웹 브라우저에서 확인
| 서비스 | URL | 설명 |
|--------|-----|------|
| **프론트엔드** | http://localhost:4000 | React 웹 UI |
| **백엔드 API** | http://localhost:8000/health | 헬스체크 |
| **H2 콘솔** | http://localhost:8000/h2-console | DB 관리 |

### API 테스트 (터미널에서)
```bash
# 백엔드 헬스체크
curl http://localhost:8000/health
# 출력: {"status":"UP"}
```

---

## 5. 일상적인 명령어 모음

### 재시작 (코드 변경 없이)
```bash
docker-compose restart
```

### 코드 변경 후 재빌드
```bash
# 프론트엔드만 수정했을 때
docker-compose up --build -d frontend

# 백엔드만 수정했을 때
docker-compose up --build -d backend

# 둘 다 수정했을 때
docker-compose up --build -d
```

### 로그 보기
```bash
# 실시간 로그
docker-compose logs -f

# 특정 서비스만
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 중지
```bash
# 일시 중지 (데이터 유지)
docker-compose down

# 완전 삭제 (데이터베이스 포함)
docker-compose down -v
```

---

## 6. 문제 해결 (Troubleshooting)

### 문제 1: "Network needs to be recreated" 에러
```bash
# 해결법
docker-compose down
docker network rm gpu-sharing-jang_gpu-sharing-network 2>/dev/null || true
docker-compose up --build -d
```

### 문제 2: 포트 충돌 (4000 또는 8000 사용 중)
```bash
# Windows PowerShell에서 포트 확인
netstat -ano | findstr :4000
netstat -ano | findstr :8000

# 사용 중인 프로세스 종료 (관리자 권한)
taskkill /PID <PID> /F
```

### 문제 3: 백엔드 응답 없음
```bash
# 1. 백엔드 로그 확인
docker-compose logs -f backend

# 2. 백엔드 재시art
docker-compose restart backend

# 3. 전체 재시작
docker-compose down && docker-compose up -d
```

### 문제 4: 프론트엔드 빌드 실패
```bash
# 캐시 없이 재빌드
docker-compose build --no-cache frontend
docker-compose up -d frontend
```

---

## 7. 프로젝트 구조 이해

### Docker 서비스 구조
```
docker-compose.yaml
├── backend (Spring Boot)
│   ├── 내부 포트: 8000
│   ├── 외부 포트: 8000
│   └── H2 데이터베이스 (파일 저장)
│
└── frontend (React + Nginx)
    ├── 내부 포트: 80
    ├── 외부 포트: 4000
    └── /api/* → backend:8000 (프록시)
```

### 네트워크 흐름
```
사용자 브라우저
    ↓
http://localhost:4000 (프론트엔드)
    ↓
/api/* 요청 → nginx 프록시 → backend:8000
```

---

## 8. 개발 팁

### 프론트엔드만 로컬로 개발 (빠른 수정)
```bash
# 1. 백엔드는 Docker로 실행
docker-compose up -d backend

# 2. 프론트엔드는 로컬 개발 서버
cd Frontend
npm install
npm run dev
# → http://localhost:5173
```

### 백엔드 API 변경 시
`Backend/src/main/java/` 아래 파일 수정 후:
```bash
docker-compose up --build -d backend
```

---

## 9. 체크리스트

처음 실행할 때 확인할 것:
- [ ] Docker Desktop 켜져 있음
- [ ] WSL Ubuntu 접속됨
- [ ] 프로젝트 폴더 위치 확인 (`pwd`)
- [ ] `docker-compose up --build -d` 실행
- [ ] 빌드 완료 대기 (5~10분)
- [ ] http://localhost:4000 접속 확인
- [ ] 로그인 페이지 표시 확인

---

## 10. 참고 문서

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 명령어](https://docs.docker.com/compose/reference/)
- 프로젝트 내 `DOCKER_GUIDE.md`, `COMMANDS.txt` 파일도 참조

---

**마지막 업데이트**: 2024년
