# GPU Sharing Backend - 구현 상태 정리

> 작성일: 2025년 4월 3일  
> 위치: `C:\Users\장성욱\Desktop\ws\GPU-sharing-Jang\Backend`

---

## 1. 기술 스택

| 구성요소 | 버전/설명 |
|---------|----------|
| Java | 21 |
| Spring Boot | 3.2.1 |
| Spring Security | JWT 기반 인증 |
| Spring Data JPA | 데이터 접근 계층 |
| 데이터베이스 | PostgreSQL (Docker 기반) |
| WebFlux | Relay/Worker 통신용 |
| 빌드 도구 | Maven |
| 컨테이너 | Docker |

---

## 2. 구현된 기능

### 2.1 인증/인가 (Auth)

| 기능 | 상태 | 설명 |
|-----|------|------|
| 회원가입 | ✅ 완료 | `POST /auth/register` - 이메일, 비밀번호, 이름으로 회원가입 |
| 로그인 | ✅ 완료 | `POST /auth/login` - JWT 토큰 발급 |
| 현재 사용자 정보 조회 | ✅ 완료 | `GET /auth/me` - JWT 토큰으로 사용자 정보 조회 |
| 비밀번호 변경 | ✅ 완료 | `POST /auth/change-password` - 현재/신규 비밀번호 변경 |

**파일 위치:**
- `@/Backend/src/main/java/com/gpu/sharing/controller/AuthController.java`
- `@/Backend/src/main/java/com/gpu/sharing/service/AuthService.java`
- `@/Backend/src/main/java/com/gpu/sharing/security/JwtTokenProvider.java`
- `@/Backend/src/main/java/com/gpu/sharing/security/JwtAuthenticationFilter.java`

### 2.2 컨테이너/세션 관리 (Container/Session)

| 기능 | 상태 | 설명 |
|-----|------|------|
| 컨테이너 생성 | ✅ 완료 | `POST /create_container` - Relay에 워커 할당 요청 → Worker에 컨테이너 생성 |
| 컨테이너 목록 조회 | ✅ 완료 | `GET /get_containers` - 사용자의 모든 세션 조회 (동기화 포함) |
| 특정 컨테이너 조회 | ✅ 완료 | `GET /get_container/{id}` - 단일 세션 상세 조회 |
| 컨테이너 삭제 | ✅ 완료 | `DELETE /delete_container/{id}` - Worker에서 컨테이너 삭제 + DB 세션 삭제 |
| 세션 동기화 | ✅ 완료 | `POST /reconcile_sessions` - DB와 Worker 상태 동기화 |

**파일 위치:**
- `@/Backend/src/main/java/com/gpu/sharing/controller/ContainerController.java`
- `@/Backend/src/main/java/com/gpu/sharing/service/ContainerService.java`
- `@/Backend/src/main/java/com/gpu/sharing/service/RelayService.java`
- `@/Backend/src/main/java/com/gpu/sharing/service/WorkerService.java`

### 2.3 클러스터 관리 (Cluster)

| 기능 | 상태 | 설명 |
|-----|------|------|
| 클러스터 목록 조회 | ✅ 완료 | `GET /get_clusters` - 모든 클러스터 정보 조회 |
| 클러스터 추가 | ✅ 완료 | `POST /add_clusters` - 새 GPU 노드(클러스터) 등록 |

**파일 위치:**
- `@/Backend/src/main/java/com/gpu/sharing/controller/ClusterController.java`
- `@/Backend/src/main/java/com/gpu/sharing/entity/Cluster.java`

### 2.4 템플릿 관리 (Container Template)

| 기능 | 상태 | 설명 |
|-----|------|------|
| 템플릿 목록 조회 | ✅ 완료 | `GET /get_templates` - 사용 가능한 이미지 목록 |
| 템플릿 추가 | ✅ 완료 | `POST /add_templates` - 새 Docker 이미지 등록 |

**파일 위치:**
- `@/Backend/src/main/java/com/gpu/sharing/controller/TemplateController.java`
- `@/Backend/src/main/java/com/gpu/sharing/entity/ContainerTemplate.java`

### 2.5 관리자 기능 (Admin)

| 기능 | 상태 | 설명 |
|-----|------|------|
| 전체 컨테이너 조회 | ✅ 완료 | `GET /admin/containers` - 모든 사용자의 컨테이너 조회 |

**파일 위치:**
- `@/Backend/src/main/java/com/gpu/sharing/controller/AdminController.java`

---

## 3. 엔티티 (Database Schema)

### 3.1 User (사용자)
```
- id (PK)
- username (unique, 50자)
- email (unique, 100자)
- password_hash
- ssh_public_key (TEXT)
- is_admin (boolean, default: false)
- created_at
- updated_at
```

### 3.2 Session (세션/컨테이너)
```
- id (PK)
- user_id (FK)
- cluster_id (FK)
- container_id (100자)
- image_name (200자, NOT NULL)
- ssh_port_mapped (integer)
- jupyter_port_mapped (integer)
- ssh_command (150자)
- ssh_password (100자)
- status (20자, default: "STARTING")
- error_msg (TEXT)
- started_at
- ended_at
- uptime_seconds (bigint, default: 0)
```

### 3.3 Cluster (GPU 클러스터)
```
- id (PK)
- name (100자)
- ip_address (100자)
- ssh_port (integer)
- gpu_name (100자)
- gpu_count (integer)
- gpu_vram_gb (integer)
- specs (JSON)
- is_active (boolean)
- status (20자)
- description (TEXT)
- created_at
- updated_at
```

### 3.4 ContainerTemplate (이미지 템플릿)
```
- id (PK)
- image_name (200자, unique, NOT NULL)
- created_at
```

---

## 4. 외부 통신 (Relay/Worker)

### 4.1 Relay Service
- **할당 요청**: `POST /allocate` → 워커 서버 URL과 클러스터 ID 반환
- WebClient 사용 (WebFlux)

### 4.2 Worker Service
- **컨테이너 생성**: `POST /containers/create`
- **컨테이너 삭제**: `DELETE /containers/{id}`
- **컨테이너 목록**: `GET /containers`

---

## 5. 설정 파일

| 파일 | 설명 |
|-----|------|
| `application.yml` | 메인 설정 (PostgreSQL 데이터베이스) |
| `docker-compose.backend.yaml` | Docker Compose 설정 |
| `Dockerfile` | 백엔드 이미지 빌드 |
| `.env` / `.env.example` | 환경변수 |
| `pom.xml` | Maven 의존성 관리 |

---

## 6. API 엔드포인트 요약

### 공개 API (인증 불필요)
| 메서드 | 엔드포인트 | 설명 |
|--------|----------|------|
| GET | `/health` | 서버 상태 확인 |
| GET | `/` | 서버 정보 |
| POST | `/auth/register` | 회원가입 |
| POST | `/auth/login` | 로그인 |


### 인증 필요 API (JWT Bearer Token)
| 메서드 | 엔드포인트 | 설명 |
|--------|----------|------|
| GET | `/auth/me` | 현재 사용자 정보 |
| POST | `/auth/change-password` | 비밀번호 변경 |
| POST | `/create_container` | 컨테이너 생성 |
| GET | `/get_containers` | 컨테이너 목록 조회 |
| GET | `/get_container/{id}` | 특정 컨테이너 조회 |
| DELETE | `/delete_container/{id}` | 컨테이너 삭제 |
| POST | `/reconcile_sessions` | 세션 동기화 |
| GET | `/get_clusters` | 클러스터 목록 |
| POST | `/add_clusters` | 클러스터 추가 |
| GET | `/get_templates` | 템플릿 목록 |
| POST | `/add_templates` | 템플릿 추가 |

### 관리자 API
| 메서드 | 엔드포인트 | 설명 |
|--------|----------|------|
| GET | `/admin/containers` | 전체 컨테이너 조회 |

---

## 7. 실행 방법

```powershell
# 1. Maven 빌드
wsl -d Ubuntu -e bash -c "cd /mnt/c/Users/장성욱/Desktop/ws/GPU-sharing-Jang/Backend && mvn clean package -DskipTests"

# 2. Docker 실행
wsl -d Ubuntu -e bash -c "cd /mnt/c/Users/장성욱/Desktop/ws/GPU-sharing-Jang/Backend && docker-compose -f docker-compose.backend.yaml up --build -d"

# 3. 로그 확인
wsl -d Ubuntu -e bash -c "cd /mnt/c/Users/장성욱/Desktop/ws/GPU-sharing-Jang/Backend && docker-compose -f docker-compose.backend.yaml logs -f"
```

---

## 8. 미구현/개선 필요 사항

| 항목 | 우선순위 | 설명 |
|-----|--------|------|
| 실시간 컨테이너 상태 업데이트 | 중간 | WebSocket으로 상태 변경 실시간 푸시 |
| GPU 사용량 모니터링 | 중간 | 클러스터 GPU 사용량 수집/표시 |
| 컨테이너 자동 종료 | 중간 | 일정 시간 사용 안 하면 자동 삭제 |
| 사용자별 리소스 제한 | 낮음 | GPU 사용 시간/수량 제한 |
| 로깅 개선 | 낮음 | structured logging, 로그 레벨 설정 |

---

## 9. 프로젝트 구조

```
Backend/
├── src/main/java/com/gpu/sharing/
│   ├── GpuSharingApplication.java    # 메인 애플리케이션
│   ├── controller/                    # REST API 컨트롤러 (6개)
│   │   ├── AuthController.java
│   │   ├── ContainerController.java
│   │   ├── ClusterController.java
│   │   ├── TemplateController.java
│   │   ├── AdminController.java
│   │   └── HealthController.java
│   ├── service/                       # 비즈니스 로직 (6개)
│   │   ├── AuthService.java
│   │   ├── ContainerService.java
│   │   ├── RelayService.java
│   │   ├── WorkerService.java
│   │   ├── InitializationService.java
│   │   └── UserService.java
│   ├── repository/                    # 데이터 접근 계층 (4개)
│   │   ├── UserRepository.java
│   │   ├── SessionRepository.java
│   │   ├── ClusterRepository.java
│   │   └── ContainerTemplateRepository.java
│   ├── entity/                        # JPA 엔티티 (5개)
│   │   ├── User.java
│   │   ├── Session.java
│   │   ├── Cluster.java
│   │   ├── ContainerTemplate.java
│   │   └── MapJsonConverter.java
│   ├── dto/                           # 데이터 전송 객체 (9개)
│   │   ├── AuthRequest.java
│   │   ├── AuthResponse.java
│   │   ├── RegisterRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── UserDto.java
│   │   ├── CreateContainerRequest.java
│   │   ├── AddClusterRequest.java
│   │   ├── AddTemplatesRequest.java
│   │   └── AdminSessionDto.java
│   ├── security/                      # JWT 인증/인가 (4개)
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── CustomUserDetailsService.java
│   └── config/                        # 설정 클래스 (3개)
│       ├── SecurityConfig.java
│       ├── JpaConfig.java
│       └── WebClientConfig.java
├── src/main/resources/
│   ├── application.yml               # 애플리케이션 설정
│   ├── application-h2.yml            # H2 설정
│   └── schema.sql (선택적)
├── docker-compose.backend.yaml       # Docker Compose
├── Dockerfile                        # Docker 이미지
├── pom.xml                          # Maven 설정
├── .env                             # 환경변수
└── .env.example                     # 환경변수 예시
```

---

## 10. 테스트 예시

### 회원가입
```bash
curl -X POST http://localhost:8001/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","name":"Test User"}'
```

### 로그인 + 컨테이너 조회
```bash
# 로그인하여 토큰 얻기
TOKEN=$(curl -s -X POST http://localhost:8001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | \
  grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# 컨테이너 목록 조회
curl http://localhost:8001/get_containers \
  -H "Authorization: Bearer $TOKEN"
```

---

## 11. 연락처/참고

- **FastAPI 프로토타입과 API 호환성 유지**
- **포트**: 8001 (백엔드)
- **CORS**: localhost:3000, localhost:5173 등 개발 서버 허용
