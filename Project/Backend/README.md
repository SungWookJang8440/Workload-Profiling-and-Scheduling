# GPU Sharing Backend - 구현 현황

## 📊 프로젝트 개요

GPU 공유 서비스 백엔드 (Spring Boot 기반)
- **상태**: 기본 기능 구현 완료
- **기술 스택**: Java 21, Spring Boot 3.2.1, Spring Security + JWT, Spring Data JPA, PostgreSQL, WebFlux

---

## ✅ 구현된 기능

### 1. 인증/인가 시스템 (Auth)
| 기능 | 상태 | 설명 |
|------|------|------|
| 회원가입 | ✅ 완료 | 이메일 기반 회원가입, 중복 체크, 고유 username 생성 |
| 로그인 | ✅ 완료 | JWT 토큰 발급, Spring Security 인증 |
| 현재 사용자 조회 | ✅ 완료 | `/auth/me` - 로그인한 사용자 정보 반환 |
| 비밀번호 변경 | ✅ 완료 | 현재 비밀번호 검증 후 변경 |

**파일**: `AuthController.java`, `AuthService.java`, JWT 관련 4개 클래스

### 2. 컨테이너 관리 (Container)
| 기능 | 상태 | 설명 |
|------|------|------|
| 컨테이너 생성 | ✅ 완료 | Relay → Worker 할당 → 컨테이너 생성 → DB 저장 |
| 컨테이너 목록 조회 | ✅ 완료 | 사용자의 모든 세션 조회 (동기화 포함) |
| 특정 컨테이너 조회 | ✅ 완료 | container_id로 단일 세션 조회 |
| 컨테이너 삭제 | ✅ 완료 | Worker에서 컨테이너 삭제 + DB 세션 삭제 |
| 세션 동기화 | ✅ 완료 | reconcile_sessions - 실제 실행 중인 컨테이너와 DB 동기화 |

**파일**: `ContainerController.java`, `ContainerService.java`

### 3. 클러스터 관리 (Cluster)
| 기능 | 상태 | 설명 |
|------|------|------|
| 클러스터 목록 조회 | ✅ 완료 | 모든 GPU 클러스터 정보 반환 |
| 클러스터 추가 | ✅ 완료 | 머신 이름/IP로 새 클러스터 등록 |

**파일**: `ClusterController.java`, `ClusterRepository.java`

### 4. 템플릿 관리 (Template)
| 기능 | 상태 | 설명 |
|------|------|------|
| 템플릿 목록 조회 | ✅ 완료 | 사용 가능한 Docker 이미지 목록 |
| 템플릿 추가 | ✅ 완료 | 단일/다중 이미지 이름으로 템플릿 등록 |

**파일**: `TemplateController.java`, `ContainerTemplateRepository.java`

### 5. 관리자 기능 (Admin)
| 기능 | 상태 | 설명 |
|------|------|------|
| 전체 컨테이너 조회 | ✅ 완료 | ADMIN 권한으로 모든 사용자의 세션 조회 |

**파일**: `AdminController.java`

### 6. 외부 연동 (Relay/Worker)
| 기능 | 상태 | 설명 |
|------|------|------|
| Relay 서버 통신 | ✅ 완료 | WebClient로 Worker 할당 요청 |
| Worker 서버 통신 | ✅ 완료 | 컨테이너 생성/삭제/조회 API 호출 |

**파일**: `RelayService.java`, `WorkerService.java`, `WebClientConfig.java`

---

## 📁 프로젝트 구조

```
Backend/src/main/java/com/gpu/sharing/
├── controller/           # 6개 컨트롤러
│   ├── AuthController.java      # 인증 API
│   ├── ContainerController.java # 컨테이너 API
│   ├── ClusterController.java   # 클러스터 API
│   ├── TemplateController.java  # 템플릿 API
│   ├── AdminController.java     # 관리자 API
│   └── HealthController.java    # 헬스체크 API
│
├── service/              # 5개 서비스
│   ├── AuthService.java         # 인증 로직
│   ├── ContainerService.java    # 컨테이너 로직
│   ├── RelayService.java        # Relay 서버 통신
│   ├── WorkerService.java       # Worker 서버 통신
│   └── InitializationService.java # 초기 데이터 설정
│
├── repository/           # 4개 리포지토리
│   ├── UserRepository.java
│   ├── SessionRepository.java
│   ├── ClusterRepository.java
│   └── ContainerTemplateRepository.java
│
├── entity/               # 5개 엔티티
│   ├── User.java
│   ├── Session.java
│   ├── Cluster.java
│   ├── ContainerTemplate.java
│   └── MapJsonConverter.java
│
├── dto/                  # 9개 DTO
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   ├── RegisterRequest.java
│   ├── ChangePasswordRequest.java
│   ├── UserDto.java
│   ├── CreateContainerRequest.java
│   ├── AddClusterRequest.java
│   ├── AddTemplatesRequest.java
│   └── AdminSessionDto.java
│
├── security/             # 4개 보안 클래스
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── CustomUserDetailsService.java
│
└── config/                 # 3개 설정
    ├── SecurityConfig.java
    ├── JpaConfig.java
    └── WebClientConfig.java
```

---

## 🔌 API 엔드포인트

### 공개 API (인증 불필요)
| 메서드 | 엔드포인트 | 설명 |
|--------|----------|------|
| GET | `/health` | 서버 상태 확인 |
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

## 🔄 동작 흐름

### 컨테이너 생성 흐름
```
1. Client → POST /create_container (JWT 인증)
2. Backend → Relay 서버: POST /allocate_worker
3. Relay → Backend: {cluster_id, server_url}
4. Backend → Worker 서버: POST /create_container
5. Worker → Backend: {container_id, ssh_command, ssh_password, ssh_port}
6. Backend → DB: Session 저장
7. Backend → Client: 생성된 컨테이너 정보 반환
```

### 세션 동기화 흐름
```
1. Client → GET /get_containers 또는 POST /reconcile_sessions
2. Backend → DB: 사용자의 모든 세션 조회
3. Backend → 각 Worker: GET /get_containers (실제 실행 중인 컨테이너 확인)
4. Backend → DB: 실제 없는 컨테이너 세션 삭제
5. Backend → Client: 동기화된 목록 반환
```

---

## ⚠️ 알려진 이슈 / 개선 필요 사항

| 이슈 | 위치 | 설명 |
|------|------|------|
| 하드코딩된 IP | `ContainerService.java:108` | 삭제 시 Worker URL이 "127.0.0.1"로 고정됨 (실제 cluster IP 사용 필요) |

---

## 🚀 실행 방법

```powershell
# 1. Maven 빌드
wsl -d Ubuntu -e bash -c "cd /mnt/c/Users/장성욱/Desktop/ws/GPU-sharing-Jang/Backend && mvn clean package -DskipTests"

# 2. Docker 실행
wsl -d Ubuntu -e bash -c "cd /mnt/c/Users/장성욱/Desktop/ws/GPU-sharing-Jang/Backend && docker-compose -f docker-compose.backend.yaml up --build -d"

# 3. 로그 확인
wsl -d Ubuntu -e bash -c "cd /mnt/c/Users/장성욱/Desktop/ws/GPU-sharing-Jang/Backend && docker-compose -f docker-compose.backend.yaml logs -f"
```

---

## 📝 테스트 예시

```bash
# 회원가입
curl -X POST http://localhost:8001/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","name":"Test User"}'

# 로그인
curl -X POST http://localhost:8001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# 컨테이너 생성 (토큰 필요)
curl -X POST http://localhost:8001/create_container \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"image_name":"pytorch/pytorch:2.0.0-cuda11.7-cudnn8-runtime"}'
```
