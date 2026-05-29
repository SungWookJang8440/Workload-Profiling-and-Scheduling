# GPU Sharing - Frontend 현재 구현 상태 및 미구현(개선) 사항

> 작성일: 2026년 4월 7일  
> 위치: `C:\Users\장성욱\Desktop\ws\GPU-sharing-Jang\Frontend\CURRENT_STATUS.md`

## 1. 🟢 현재 구현 완료된 부분 (Implemented)

**① 전역 상태 관리 및 API 통신 연동**
- `axios` 기반의 `api.ts` 파일에서 로그인(JWT 헤더 포함) 및 컨테이너, 클러스터, 템플릿 CRUD 요청 연동 완료.
- `Zustand` (`store.ts`)를 이용하여 전역적으로 `Auth`, `Container`, `Cluster`, `Template` 상태를 원활히 관리 중.

**② 페이지 및 라우팅 (Pages & Route)**
- `react-router-dom` 기본 설정 및 사용자 경험 개선. 
- UI 라이브러리 `shadcn/ui` 및 `Tailwind CSS`를 사용한 다크 모드, Glass Morphism 디자인.
- **구현 페이지**:
  - `Login`, `Register`: 회원가입 및 JWT 발급 로그인.
  - `Dashboard`: 내 컨테이너 목록 출력 및 생성, 제거, SSH 명령어 확인 UI.
  - `Clusters`: 가용 GPU 클러스터 목록 출력 뷰.
  - `Templates`: 사용 가능한 도커 템플릿 출력창.
  - `Profile`: 계정 정보 및 비밀번호 변경.

---

## 2. 🔴 미구현 부분 및 웹 단의 비연결점 (Unimplemented & Missing Links)

**① 관리자(Admin) UI 페이지 완전 부재**
- **연결 누락**: 프론트엔드 `api.ts`에는 `api.getAllContainers()` 함수가 구현되어 백엔드의 `/admin/containers` 호출 준비가 완료되었습니다.
- **미구현**: 하지만 실제로 이 데이터를 호출하여 보여줄 `admin.tsx` (관리자 대시보드) 페이지 템플릿과 UI 코드가 프론트엔드에 **전혀 구현되어 있지 않습니다.** 이로 인해 관리자 기능을 사용할 수 없습니다.

**② 서버 상태 수동 동기화 한계 (실시간 업데이트 미구현)**
- 백엔드에서 WebSocket/SSE를 지원하지 않으므로, 컨테이너를 생성한 후 상태(`STARTING` -> `RUNNING` 등)를 확인하려면 사용자가 수동으로 새로고침(`reconcileSessions`) 하거나 화면을 다시 방문해야 합니다. 프론트엔드 레벨에서의 `Polling` (몇 초마다 `fetchContainers` 수행) 로직도 적용되어 있지 않습니다.

**③ Web SSH 터미널 통합 미구현**
- 컨테이너가 생성되면 SSH 연결 포워딩 명령어만 UI로 복사할 수 있게 제공되고 있습니다.
- 브라우저 상에서 직접 컨테이너쉘로 접근하는 웹 기반 SSH (예: `xterm.js` 등) 연동 UI가 미구현 상태입니다. GPU 컨테이너 활용 편의성이 떨어집니다.

**④ 기능 삭제 및 세부 수정 UI 부재**
- **클러스터 관리**: 클러스터 '추가' API 호출은 가능하지만, 기존 클러스터를 삭제하거나 수정하는 기능/UI가 없습니다.
- **템플릿 관리**: 추가 기능은 있지만, 잘못 추가된 템플릿을 삭제·숨김 처리하는 UI가 없습니다.
