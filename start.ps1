# ============================================================
# Workload Profiling & GPU Scheduling - 원클릭 실행 스크립트
# 로컬 Windows 환경 (Java 21 + Maven + Node.js 직접 실행)
# ============================================================

param(
    [switch]$Stop   # .\start.ps1 -Stop 으로 실행하면 서버 종료
)

$ErrorActionPreference = "SilentlyContinue"

$JAVA_HOME_PATH  = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$MAVEN_PATH      = "C:\Users\csu84\Desktop\Workspace\maven\apache-maven-3.9.6\bin\mvn.cmd"
$BACKEND_DIR     = "$PSScriptRoot\Project\Backend"
$FRONTEND_DIR    = "$PSScriptRoot\Project\Frontend"
$BACKEND_PORT    = 8000
$FRONTEND_PORT   = 5173

function Write-Step($msg) { Write-Host "`n$msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "  ✅ $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "  ⚠️  $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "  ❌ $msg" -ForegroundColor Red }

# ─────────── 종료 모드 ───────────
if ($Stop) {
    Write-Step "[종료] 실행 중인 서버를 종료합니다..."
    foreach ($port in @($BACKEND_PORT, $FRONTEND_PORT)) {
        $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn) {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
            Write-OK "포트 $port 서버 종료 완료"
        } else {
            Write-Warn "포트 $port 에서 실행 중인 서버 없음"
        }
    }
    exit 0
}

# ─────────── 사전 조건 확인 ───────────
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  MCDM GPU 스케줄러 - 로컬 실행 스크립트" -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta

Write-Step "[1/4] 사전 조건 확인..."

# Java 확인
if (Test-Path "$JAVA_HOME_PATH\bin\java.exe") {
    Write-OK "Java 21 확인됨 ($JAVA_HOME_PATH)"
} else {
    Write-Err "Java 21을 찾을 수 없습니다: $JAVA_HOME_PATH"
    Write-Host "       → https://adoptium.net 에서 Java 21 설치 후 재시도하세요." -ForegroundColor Yellow
    exit 1
}

# Maven 확인
if (Test-Path $MAVEN_PATH) {
    Write-OK "Maven 확인됨"
} else {
    Write-Err "Maven을 찾을 수 없습니다: $MAVEN_PATH"
    Write-Host "       → .\run-local.ps1 을 먼저 한번 실행하면 Maven을 자동 다운로드합니다." -ForegroundColor Yellow
    exit 1
}

# Node.js 확인
$nodeVersion = node --version 2>$null
if ($nodeVersion) {
    Write-OK "Node.js 확인됨 ($nodeVersion)"
} else {
    Write-Err "Node.js를 찾을 수 없습니다."
    Write-Host "       → https://nodejs.org 에서 LTS 버전 설치 후 재시도하세요." -ForegroundColor Yellow
    exit 1
}

# ─────────── 포트 정리 ───────────
Write-Step "[2/4] 포트 중복 확인 및 정리..."
foreach ($port in @($BACKEND_PORT, $FRONTEND_PORT)) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Warn "포트 $port 기존 프로세스 종료됨"
        Start-Sleep -Seconds 1
    }
}
Write-OK "포트 정리 완료"

# ─────────── 백엔드 실행 ───────────
Write-Step "[3/4] 백엔드 서버 시작 (Spring Boot, 포트 $BACKEND_PORT)..."
$backendCmd = @"
`$env:JAVA_HOME = '$JAVA_HOME_PATH'
`$env:Path = `$env:JAVA_HOME + '\bin;' + `$env:Path
Set-Location '$BACKEND_DIR'
Write-Host '[백엔드] Spring Boot 기동 중...' -ForegroundColor Cyan
& '$MAVEN_PATH' spring-boot:run
"@

Start-Process powershell `
    -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd `
    -WindowStyle Normal
Write-OK "백엔드 프로세스 시작됨 (새 창에서 로그 확인 가능)"

# ─────────── 프론트엔드 실행 ───────────
Write-Step "[4/4] 프론트엔드 서버 시작 (Vite, 포트 $FRONTEND_PORT)..."
$frontendCmd = @"
Set-Location '$FRONTEND_DIR'
Write-Host '[프론트엔드] npm install 확인 중...' -ForegroundColor Cyan
if (-not (Test-Path 'node_modules')) { npm install }
Write-Host '[프론트엔드] Vite dev 서버 기동 중...' -ForegroundColor Cyan
npm run dev
"@

Start-Process powershell `
    -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $frontendCmd `
    -WindowStyle Normal
Write-OK "프론트엔드 프로세스 시작됨 (새 창에서 로그 확인 가능)"

# ─────────── 완료 안내 ───────────
Write-Host "`n============================================================" -ForegroundColor Magenta
Write-Host "  실행 완료! 아래 주소로 접속하세요." -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "  🌐 대시보드:        http://localhost:$FRONTEND_PORT" -ForegroundColor White
Write-Host "  🤖 스케줄러 화면:   http://localhost:$FRONTEND_PORT/scheduler" -ForegroundColor White
Write-Host "  🔧 백엔드 헬스:     http://localhost:$BACKEND_PORT/health" -ForegroundColor White
Write-Host ""
Write-Host "  ⏳ 백엔드(Spring Boot)가 완전히 켜지려면 약 30~60초 소요됩니다." -ForegroundColor Yellow
Write-Host "  🛑 종료하려면:      .\start.ps1 -Stop" -ForegroundColor Yellow
Write-Host ""
