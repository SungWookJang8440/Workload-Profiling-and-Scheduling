# GPU Sharing Frontend

> React + TypeScript + TailwindCSS + shadcn/ui 기반 GPU 공유 플랫폼 프론트엔드

## 기술 스택

- **React 18** - UI 라이브러리
- **TypeScript** - 타입 안정성
- **Vite** - 빌드 도구
- **TailwindCSS** - 스타일링
- **shadcn/ui** - UI 컴포넌트
- **Zustand** - 상태 관리
- **React Router** - 라우팅
- **Axios** - HTTP 클라이언트
- **Framer Motion** - 애니메이션

## 디자인 특징

- **다크 모드 기본** - AI/GPU 서비스에 적합한 다크 테마
- **보라색-파란색 그라데이션** - 현대적인 액센트 컬러
- **Glass Morphism** - 반투명 블러 효과
- **Gradient Borders** - 호버 시 그라데이션 테두리 효과
- **Glow Effects** - 네온 스타일 발광 효과
- **반응형 디자인** - 모바일/태블릿/데스크탑 지원

## 기능

### 인증
- 로그인 / 회원가입
- JWT 토큰 기반 인증
- 비밀번호 변경

### 대시보드
- 컨테이너 상태 통계
- 실시간 컨테이너 목록
- SSH 접속 정보 표시
- 컨테이너 생성/삭제
- 세션 동기화

### 클러스터 관리
- GPU 노드 목록 조회
- 클러스터 등록
- GPU 정보 표시 (모델, VRAM, 개수)

### 템플릿 관리
- Docker 이미지 목록
- 템플릿 등록
- 템플릿으로 컨테이너 빠른 생성

### 프로필
- 사용자 정보 표시
- 비밀번호 변경

## 프로젝트 구조

```
Frontend/
├── src/
│   ├── components/
│   │   ├── ui/              # shadcn/ui 컴포넌트
│   │   │   ├── button.tsx
│   │   │   ├── card.tsx
│   │   │   ├── dialog.tsx
│   │   │   ├── input.tsx
│   │   │   ├── select.tsx
│   │   │   ├── badge.tsx
│   │   │   ├── avatar.tsx
│   │   │   ├── label.tsx
│   │   │   ├── separator.tsx
│   │   │   ├── tooltip.tsx
│   │   │   └── dropdown-menu.tsx
│   │   ├── layout.tsx       # 메인 레이아웃 (사이드바)
│   │   ├── protected-route.tsx
│   │   └── toasts-container.tsx
│   ├── pages/
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   ├── dashboard.tsx
│   │   ├── clusters.tsx
│   │   ├── templates.tsx
│   │   └── profile.tsx
│   ├── lib/
│   │   ├── api.ts           # API 클라이언트
│   │   ├── store.ts         # Zustand 스토어
│   │   ├── toast.ts         # Toast 알림
│   │   └── utils.ts         # 유틸리티
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
└── postcss.config.js
```

## 설치 및 실행

### 1. 의존성 설치

```bash
cd Frontend
npm install
```

### 2. 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env
```

```env
VITE_API_URL=http://localhost:8001
```

### 3. 개발 서버 실행

```bash
npm run dev
```

서버가 `http://localhost:5173`에서 실행됩니다.

### 4. 프로덕션 빌드

```bash
npm run build
```

## API 연동

백엔드 API와 자동으로 연동됩니다. Vite 프록시 설정:

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8001',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  },
}
```

## 페이지별 기능

### 로그인 (`/login`)
- 이메일/비밀번호 입력
- JWT 토큰 저장
- 로그인 후 대시보드 리다이렉트

### 대시보드 (`/dashboard`)
- 컨테이너 통계 카드
- 활성/전체 컨테이너 수
- 템플릿 수
- 컨테이너 카드 그리드
- SSH 명령어 복사
- 컨테이너 삭제
- 새 컨테이너 생성 다이얼로그

### 클러스터 (`/clusters`)
- 클러스터 목록
- GPU 정보 표시
- 클러스터 추가 다이얼로그

### 템플릿 (`/templates`)
- Docker 이미지 목록
- 이미지 이름 복사
- 템플릿으로 컨테이너 생성
- 새 템플릿 추가

### 프로필 (`/profile`)
- 사용자 정보 표시
- 아바타
- 비밀번호 변경 폼

## 스크린샷

*추가 예정*

## 라이선스

MIT License
