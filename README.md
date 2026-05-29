# Workload Profiling and Scheduling (GPU Sharing Project)

이 프로젝트는 GPU 공유 인프라 환경에서 워크로드 프로파일링 및 효율적인 GPU 자원 스케줄링을 제어하기 위한 협업 저장소입니다.

## 📂 프로젝트 구조 (Directory Structure)

팀원들과 효율적으로 협업하기 위해 다음과 같이 표준 디렉터리 구조를 설계하였습니다.

```
.
├── Project/                   # 공동 작업 및 실제 프로젝트 소스코드
│   ├── Infra/                 # 인프라 설정, 배포용 Docker 설정 및 셋업 가이드 문서
│   │   ├── docker-compose.yaml
│   │   ├── START_GUIDE.md
│   │   └── ... (기타 스크립트 및 가이드)
│   ├── Algorithm/             # GPU 자원 공유 및 스케줄링 알고리즘 연구 및 소스코드
│   ├── Backend/               # Spring Boot 기반의 백엔드 애플리케이션
│   └── Frontend/              # React & Vite 기반의 프론트엔드 대시보드 애플리케이션
│
├── Archive/                   # 개인 보관 및 이전 히스토리 파일 (레거시 백업용)
│   ├── 2026S_SW중심대학_...pdf
│   ├── SYSTEM_PROGRESS_REPORT.md
│   └── ... (개인 작성 스크립트 등)
│
└── .gitignore                 # 빌드 산출물, 노드 모듈 및 기밀(env) 파일을 제외하기 위한 Git 설정
```

---

## 🚀 빠른 시작 (Quick Start)

### 1. 인프라 및 도커 환경 구성
이 프로젝트의 빌드, 배포 환경 및 인프라 설정은 **`Project/Infra`** 폴더에 정리되어 있습니다.
자세한 실행 가이드는 아래 문서를 참고해주세요.
* 📖 [인프라 셋업 가이드 (START_GUIDE.md)](Project/Infra/START_GUIDE.md)
* 📖 [환경 모드 및 실행 가이드 (ENVIRONMENT_MODES_GUIDE.md)](Project/Infra/ENVIRONMENT_MODES_GUIDE.md)

### 2. 백엔드 개발
* 백엔드는 Spring Boot(Java 21, Maven)로 개발되었습니다.
* 📖 [백엔드 상세 가이드 (Project/Backend/README.md)](Project/Backend/README.md)

### 3. 프론트엔드 개발
* 프론트엔드는 React, Vite, TS, Tailwind CSS로 구성되어 있으며 실시간 GPU 상태를 표시합니다.
* 📖 [프론트엔드 상세 가이드 (Project/Frontend/README.md)](Project/Frontend/README.md)

---

## 🤝 협업 규칙 (Collaboration Guide)

1. **커밋 메시지 규칙**: 깃허브 커밋 시 명확한 접두사(`feat:`, `fix:`, `docs:`, `refactor:`)를 사용합니다.
2. **브랜치 전략**: `main` 브랜치는 항상 빌드 가능한 상태로 유지하며, 기능 추가는 작업 브랜치(`feature/feature-name`)에서 작업 후 Pull Request(PR)를 거쳐 병합합니다.
3. **환경 변수 관리**: `.env` 파일과 같이 기밀 정보나 로컬 설정이 포함된 파일은 절대 깃허브에 직접 업로드하지 않습니다. 필요시 예시 템플릿(`.env.example`)을 제공하여 각자 환경에 맞춰 작성하도록 유도합니다.
