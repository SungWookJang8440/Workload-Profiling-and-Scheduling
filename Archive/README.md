# GPU Sharing Service Platform

AI 및 ML 머신러닝 개발자들을 위한 **GPU 리소스 공유 및 컨테이너 할당 플랫폼**입니다.
이 프로젝트는 Spring Boot 기반의 강력한 백엔드와 React(Vite) 기반의 모던 프론트엔드가 결합된 풀스택 어플리케이션입니다.

---

## 🏗 프로젝트 구조

```text
GPU-sharing-Jang/
├── Backend/                    # Spring Boot REST API 서버
│   ├── src/                    # 비즈니스 로직, 엔티티, 컨트롤러
│   ├── Dockerfile             
│   ├── docker-compose.backend.yaml
│   └── README.md
│
├── Frontend/                   # React + Vite 프론트엔드 대시보드
│   ├── src/                    # UI 컴포넌트, 페이지, 상태관리(Zustand)
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yaml         # 백엔드 + 프론트엔드 + DB 통합 실행 설정
└── README.md                   # 현재 파일
```

---

## 🛠 기술 스택

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.1
- **Security**: Spring Security + JWT Token Authentication
- **Database**: PostgreSQL 15 (Docker) & Spring Data JPA
- **API Communication**: WebFlux (외부 워커 노드 및 릴레이 연동 예정)

### Frontend
- **Framework**: React 19 + TypeScript + Vite
- **Styling**: Tailwind CSS + Shadcn-UI (Lucide Icons)
- **State Management**: Zustand
- **HTTP Client**: Axios (with Interceptors)

### Infrastructure
- **Containerization**: Docker & Docker Compose

---

## 🚀 빠른 시작 (통합 실행 가이드)

### 1️⃣ 배포용 모드 (Docker Compose) - 권장
아래 명령어를 통해 **백엔드(Spring Boot), 프론트엔드(Nginx), 데이터베이스(PostgreSQL)**를 한 번에 백그라운드 환경에서 구동할 수 있습니다.

```bash
# 기존 컨테이너 및 볼륨 초기화 후 빌드 및 백그라운드 실행
docker compose down -v
docker compose up -d --build
```
* 접속 주소: `http://localhost:4000` (프론트엔드 대시보드)
* API  주소: `http://localhost:8000` (백엔드 서버)

### 2️⃣ 개발용 모드 (Hot-Reloading)
프론트엔드 UI를 수정하고 실시간으로 반영되는 것을 확인하면서 작업하고 싶을 때 사용합니다.

**1. 터미널 1: 브라우저용 (프론트엔드)**
```bash
cd Frontend
npm install
npm run dev
```
* 접속 주소: `http://localhost:5173`

**2. 터미널 2: 서버 및 DB용 (백엔드)**
```bash
# DB 및 백엔드 서버는 도커로 띄우거나 IDE(IntelliJ 등)에서 직접 실행하세요.
docker compose up -d backend postgres
```

### 3️⃣ 소스 코드 수정 후 재빌드 가이드
백엔드(`Backend/`) 또는 프론트엔드(`Frontend/`)에 코드 변경 사항이 생기면 도커 이미지를 새로 빌드해야 반영됩니다.

**상황 A) 백엔드 코드만 수정한 경우**
```bash
docker compose up -d --build backend
```

**상황 B) 프론트엔드 코드만 수정한 경우**
```bash
docker compose up -d --build frontend
```

**상황 C) 전체를 아예 새로고침(초기화)하여 재빌드하고 싶을 때**
```bash
docker compose down
docker compose up -d --build --force-recreate
```

---

## 🔌 주요 API 엔드포인트

서버와 통신할 때 사용하는 핵심 주소 목록입니다.
*(기본 URL: `http://localhost:8000`)*

- **인증(Auth)**
  - `POST /auth/register` : 회원가입
  - `POST /auth/login` : 로그인 (JWT 발급 - `accessToken`)
  - `GET /auth/me` : 내 정보 조회
- **컨테이너(Container)**
  - `GET /get_containers` : 내 할당 컨테이너 전체 목록
  - `POST /create_container` : 새 GPU 컨테이너 할당 요청
  - `DELETE /delete_container/{id}` : 컨테이너 제거
- **클러스터 및 템플릿**
  - `GET /get_clusters` : 가용 GPU 노드 목록 조회
  - `GET /get_templates` : 사용할 수 있는 AI 환경(PyTorch 등) 모델 이미지 조회
- **헬스 체크(Health Check)**
  - `GET /health` : 서버 데몬 생존 확인

---

## 📝 라이선스

이 프로젝트는 MIT License 하에 배포됩니다.
