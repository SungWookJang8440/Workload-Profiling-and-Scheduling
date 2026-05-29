# GPU Sharing - Backend 현재 구현 상태 및 미구현(개선) 사항

> 작성일: 2026년 4월 7일  
> 위치: `C:\Users\장성욱\Desktop\ws\GPU-sharing-Jang\Backend\CURRENT_STATUS.md`

## 1. 🟢 현재 구현 완료된 부분 (Implemented)

**① 인증/인가 (Auth)**
- JWT 기반 사용자 회원가입, 로그인, 정보 조회, 비밀번호 변경 API (`AuthController`)
- `Spring Security` 및 `JwtAuthenticationFilter`를 이용한 토큰 검증 완료.

**② 컨테이너 ও 세션 관리 (Container/Session)**
- `Relay` 서버 거쳐 `Worker` 서버와 연동 후 컨테이너 생성 흐름 구현.
- 사용자의 컨테이너 목록 조회 및 개별 조회 API.
- 컨테이너 삭제 (Worker + DB 동시 삭제) 및 `reconcile_sessions`를 통한 동기화.

**③ 클러스터 및 템플릿 (Cluster/Template)**
- GPU 노드(클러스터) 목록 조회 및 추가 API (`ClusterController`).
- Docker 컨테이너 생성에 사용될 템플릿(이미지) 조회 및 추가 API (`TemplateController`).

**④ 관리자 (Admin)**
- 전체 사용자의 컨테이너 목록을 조회할 수 있는 `/admin/containers` API 구현.

**⑤ 인프라 구성**
- H2 기반 테스트 및 단일 환경 구성, `docker-compose.backend.yaml` 도커 설정 추가. 
- WebClient를 이용한 `Relay` / `Worker` 비동기 외부 서버 통신 설정.

---

## 2. 🔴 미구현 부분 및 연결 안 된 상태 (Unimplemented & Missing Links)

**① 실시간 통신 및 상태 업데이트 (WebSocket / SSE 부재)**
- 현재 컨테이너 상태 변경 (STARTING -> RUNNING)에 대한 실시간 푸시가 지원되지 않습니다. 클라이언트 측에서 Polling(주기적 요청)이나 수동 새로고침에 의존해야 합니다.

**② 하드코딩된 Worker 주소 문제 (중요)**
- `ContainerService.java` 등의 코드 내에서 Worker 서버의 IP/주소가 `127.0.0.1` 등으로 하드코딩 되어 있는 부분이 있어, 실제 다중 Worker 환경(클러스터) 연동 시 배포 및 라우팅 에러가 발생할 수 있습니다.

**③ 시스템 통합 모니터링/로깅 로그 부재**
- 클러스터의 실제 GPU 사용량/통계(VRAM 사용량 등)를 수집하여 제공하는 기능이 구현되어 있지 않습니다.
- 유저별 리소스 제한 (할당 가능한 최대 GPU 수 등) 정책 로직이 누락되어 있습니다.

**④ 미비한 관리자 (Admin) 컨트롤러 연결**
- 백엔드에 Admin API(`GET /admin/containers`)는 존재하지만, 추가적인 관리(사용자 삭제, 강제 컨테이너 종료, 클러스터 삭제 등) API가 모두 미구현 상태입니다.

**⑤ PostgreSQL 마이그레이션 완료 (H2 제거)**
- 테스트 및 로컬 환경에서 사용되던 H2 기반 데이터베이스 설정을 소스코드 및 의존성에서 완전히 제거하였습니다.
- 이제 모든 환경(로컬/테스트 포함)에서 Docker Compose 기반의 PostgreSQL 환경만을 단일 장애포인트 없이 깔끔하게 바라보게 됩니다.
