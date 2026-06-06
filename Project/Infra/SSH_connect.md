# SSH Connection Information

## Server 1 (GPU 4090 - Instance 39265510)
- **Public IP**: 194.14.47.19
- **Machine Copy Port**: 22099
- **Direct SSH Port**: 22059
- **Proxy SSH Port**: 25511 (ssh3.vast.ai)
- **Local IPs**: 192.168.50.10, 172.17.0.1
- **Username**: root (기본값)
- **Password/Key**: `~/.ssh/id_ed25519` (SSH 키 사용, 비밀번호 없음)

### 접속 명령어 (터미널/PowerShell 입력)
**Direct SSH Connect (권장):**
```bash
ssh -p 22059 root@194.14.47.19 -L 8080:localhost:8080
```

**Proxy SSH Connect (Direct가 막혔을 때):**
```bash
ssh -p 25511 root@ssh3.vast.ai -L 8080:localhost:8080
```

# 🖥️ RTX PRO 6000 연구실 PC SSH 접속 가이드

연구실의 RTX PRO 6000 서버에 SSH로 접속하여 작업하기 위한 공식 가이드라인입니다. 본 문서를 참고하여 터미널(CLI) 접속 및 VS Code를 통한 원격 개발 환경(Remote-SSH)을 설정할 수 있습니다.

---

## 🔑 SSH 접속 정보

| 항목 | 정보 |
| :--- | :--- |
| **사용자명 (Name)** | `sslab` |
| **비밀번호 (Password)** | `sslab1!2` |
| **IP 주소 (Host)** | `155.230.118.52` |
| **포트 (Port)** | `22345` |

> [!IMPORTANT]
> 접속한 후에는 **`~/Documents`** 폴더 아래에 개별 작업용 폴더를 생성하여 모든 작업을 진행해야 합니다. 
> 시스템 루트 디렉토리나 타인의 폴더를 수정하지 않도록 주의해주세요.

---

## 🛠️ 접속 방법

### 방법 1: 터미널 (CLI)에서 직접 접속 (가장 간단함)

윈도우 PowerShell, 명령 프롬프트(CMD) 또는 Git Bash를 열고 아래 명령어를 실행합니다.

```bash
ssh sslab@155.230.118.52 -p 22345
```

1. **지문 등록 경고**: 처음 접속할 경우 `Are you sure you want to continue connecting (yes/no/[fingerprint])?` 라는 메시지가 나옵니다. `yes`를 입력하고 Enter를 누릅니다.
2. **비밀번호 입력**: `sslab1!2`를 입력합니다. (비밀번호를 입력할 때 보안을 위해 화면에는 아무 글자도 표시되지 않으니 오타 없이 입력 후 Enter를 누르시면 됩니다.)
3. **작업 폴더 생성 및 이동**:
   ```bash
   # Documents 폴더로 이동
   cd ~/Documents

   # 본인 이름이나 이니셜 등으로 작업 폴더 생성
   mkdir my-workspace   # (예: mkdir jang-work)
   cd my-workspace
   ```

---

### 방법 2: VS Code (Remote - SSH) 연동 (추천 - 가장 편리한 개발 방법)

VS Code에서 마치 로컬 컴퓨터처럼 원격 서버의 코드를 편집하고 실행할 수 있는 가장 추천하는 개발 연동 방식입니다.

#### 1단계: 확장 프로그램(Extension) 설치
1. VS Code를 실행합니다.
2. 왼쪽 사이드바의 **블록 모양 아이콘 (Extensions)** 을 클릭합니다 (단축키: `Ctrl + Shift + X`).
3. 검색창에 **`Remote - SSH`**를 검색하여 Microsoft에서 배포한 공식 확장을 설치(Install)합니다.

#### 2단계: 호스트 등록
1. VS Code 왼쪽 최하단의 초록색 아이콘 **`><`** (원격 창 열기)을 클릭하거나, 단축키 `Ctrl + Shift + P`를 누르고 **`Remote-SSH: Connect to Host...`**를 선택합니다.
2. **`Add New SSH Host...`**를 클릭합니다.
3. 아래의 SSH 연결 명령어를 입력하고 Enter를 누릅니다:
   ```bash
   ssh sslab@155.230.118.52 -p 22345
   ```
4. 설정을 저장할 SSH 구성 파일 경로를 선택합니다 (보통 첫 번째인 `C:\Users\<사용자명>\.ssh\config`를 선택합니다).

#### 3단계: 서버 연결 및 비밀번호 입력
1. 다시 한번 `Ctrl + Shift + P` -> **`Remote-SSH: Connect to Host...`**를 클릭하거나, 오른쪽 아래 알림창의 **Connect**를 클릭합니다.
2. 목록에 추가된 **`155.230.118.52`**를 선택합니다.
3. 새로운 VS Code 창이 열리며 원격 서버의 OS 유형을 묻는다면 **`Linux`**를 선택합니다.
4. 비밀번호 입력창이 나오면 **`sslab1!2`**를 입력하고 Enter를 누릅니다.

#### 4단계: 작업 폴더 열기
1. 원격 서버 연결이 성공하면 왼쪽 최하단 표시가 `SSH: 155.230.118.52`로 변경됩니다.
2. VS Code 메뉴에서 **`File` > `Open Folder`** (폴더 열기)를 클릭합니다.
3. 상단 경로 입력 창에 **`/home/sslab/Documents`** 경로를 선택하고 OK를 누릅니다.
4. 비밀번호를 다시 물어보면 **`sslab1!2`**를 입력합니다.
5. 이제 원격 서버 내에 작업 폴더를 생성하고 자유롭게 소스코드를 업로드하거나 Git을 연동해 코딩을 시작할 수 있습니다!

---

### 방법 3: 파일 전송 및 GUI 툴 사용 (MobaXterm, FileZilla 등)

코드 외에 대용량 데이터셋이나 빌드된 파일을 그래픽 화면(GUI)으로 손쉽게 업로드/다운로드하고 싶다면 아래 프로그램을 이용하세요.

* **추천 프로그램**: [MobaXterm](https://mobaxterm.mobatek.net/) 또는 [FileZilla](https://filezilla-project.org/)
* **세션 설정**:
  * **Session Type**: SFTP (또는 SSH)
  * **Host (IP)**: `155.230.118.52`
  * **Username**: `sslab`
  * **Port**: `22345`
  * **Password**: `sslab1!2`
* 접속 후 좌측의 파일 탐색기를 통해 드래그 앤 드롭으로 파일을 업로드하거나 다운로드할 수 있습니다.

---

## 🚨 주의사항 및 유용한 팁

1. **백그라운드 실행 (nohup, tmux)**:
   * 딥러닝 학습 등 장시간 실행되는 프로세스는 터미널 창을 닫으면 함께 종료됩니다. 반드시 `tmux`나 `nohup`을 이용하여 백그라운드에서 백엔드나 모델 스케줄러를 가동하세요.
   * **tmux 초간단 사용법**:
     ```bash
     # 새로운 가상 세션 생성
     tmux new -s my-session
     
     # 백엔드 서버 또는 딥러닝 코드 실행
     # (여기서 서버 구동 등 수행...)
     
     # 세션에서 빠져나오기 (백그라운드 유지): 
     # Ctrl + B를 동시에 누른 뒤 손을 떼고 바로 D 누르기
     
     # 다시 원래 세션으로 들어가서 확인하기
     tmux attach -t my-session
     ```
2. **Git 코드 공유**:
   * 원격 서버에서 프로젝트 리포지토리(`GPU-sharing-Jang`)를 연동하여 소스코드를 싱크하려면 `~/Documents/<작업폴더>`로 이동하여 Git Clone을 수행하는 것을 권장합니다:
     ```bash
     git clone <Repository_URL>
     ```
