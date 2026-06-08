# ============================================================
# Workload Profiling & GPU Scheduling - Startup Script
# Local Windows Environment (Java 21 + Maven + Node.js)
# ============================================================

param(
    [switch]$Stop   # Run ".\start.ps1 -Stop" to stop the servers
)

$ErrorActionPreference = "SilentlyContinue"

$JAVA_HOME_PATH  = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$MAVEN_PATH      = "C:\Users\csu84\Desktop\Workspace\maven\apache-maven-3.9.6\bin\mvn.cmd"
$BACKEND_DIR     = "$PSScriptRoot\Project\Backend"
$FRONTEND_DIR    = "$PSScriptRoot\Project\Frontend"
$BACKEND_PORT    = 8000
$FRONTEND_PORT   = 5173

function Write-Step($msg) { Write-Host "`n$msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "  [WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "  [ERROR] $msg" -ForegroundColor Red }

# ----------- STOP MODE -----------
if ($Stop) {
    Write-Step "[STOP] Stopping running servers..."
    foreach ($port in @($BACKEND_PORT, $FRONTEND_PORT)) {
        $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn) {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
            Write-OK "Port $port server stopped successfully."
        } else {
            Write-Warn "Port $port has no running server."
        }
    }
    exit 0
}

# ----------- START MODE -----------
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  MCDM GPU Scheduler - Local Startup Script" -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta

Write-Step "[1/4] Checking prerequisites..."

# Verify Java
if (Test-Path "$JAVA_HOME_PATH\bin\java.exe") {
    Write-OK "Java 21 found ($JAVA_HOME_PATH)"
} else {
    Write-Err "Java 21 not found: $JAVA_HOME_PATH"
    Write-Host "       -> Please install Java 21 from https://adoptium.net and retry." -ForegroundColor Yellow
    exit 1
}

# Verify Maven & Auto-Download
$mavenDir = "C:\Users\csu84\Desktop\Workspace\maven"
if (-not (Test-Path $MAVEN_PATH)) {
    Write-Warn "Maven not found. Downloading light Maven..."
    $zipPath = "C:\Users\csu84\Desktop\Workspace\maven.zip"
    try {
        if (-not (Test-Path $mavenDir)) {
            New-Item -ItemType Directory -Path $mavenDir -Force | Out-Null
        }
        Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile $zipPath
        Expand-Archive -Path $zipPath -DestinationPath $mavenDir -Force
        Remove-Item -Path $zipPath -Force
        Write-OK "Maven downloaded and configured successfully!"
    } catch {
        Write-Err "Failed to download Maven: $_"
        exit 1
    }
} else {
    Write-OK "Maven found"
}

# Verify Node.js
$nodeVersion = node --version 2>$null
if ($nodeVersion) {
    Write-OK "Node.js found ($nodeVersion)"
} else {
    Write-Err "Node.js not found."
    Write-Host "       -> Please install LTS version from https://nodejs.org and retry." -ForegroundColor Yellow
    exit 1
}

# ----------- PORT CLEARING -----------
Write-Step "[2/4] Checking and clearing duplicate ports..."
foreach ($port in @($BACKEND_PORT, $FRONTEND_PORT)) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Warn "Port $port existing process terminated."
        Start-Sleep -Seconds 1
    }
}
Write-OK "Ports cleared."

# ----------- RUN BACKEND -----------
Write-Step "[3/4] Starting backend server (Spring Boot, Port $BACKEND_PORT)..."
$backendCmd = @"
`$env:JAVA_HOME = '$JAVA_HOME_PATH'
`$env:Path = '$JAVA_HOME_PATH\bin;' + `$env:Path
Set-Location '$BACKEND_DIR'
Write-Host '[Backend] Spring Boot starting...' -ForegroundColor Cyan
& '$MAVEN_PATH' spring-boot:run
"@
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd -WindowStyle Normal
Write-OK "Backend process started (Logs can be checked in the new window)"

# ----------- RUN FRONTEND -----------
Write-Step "[4/4] Starting frontend server (Vite, Port $FRONTEND_PORT)..."
$frontendCmd = @"
Set-Location '$FRONTEND_DIR'
Write-Host '[Frontend] Checking npm install...' -ForegroundColor Cyan
if (-not (Test-Path 'node_modules')) { npm install }
Write-Host '[Frontend] Starting Vite dev server...' -ForegroundColor Cyan
npm run dev
"@

Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $frontendCmd -WindowStyle Normal
Write-OK "Frontend process started (Logs can be checked in the new window)"

# ----------- COMPLETE -----------
Write-Host "`n============================================================" -ForegroundColor Magenta
Write-Host "  Startup Complete! Please access via the URLs below." -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "  🌐 Dashboard:        http://localhost:$FRONTEND_PORT" -ForegroundColor White
Write-Host "  🤖 Scheduler Screen: http://localhost:$FRONTEND_PORT/scheduler" -ForegroundColor White
Write-Host "  🔧 Backend Health:   http://localhost:$BACKEND_PORT/health" -ForegroundColor White
Write-Host ""
Write-Host "  ⏳ It takes about 30-60 seconds for the backend to start fully." -ForegroundColor Yellow
Write-Host "  🛑 To stop the servers:  .\start.ps1 -Stop" -ForegroundColor Yellow
Write-Host ""
