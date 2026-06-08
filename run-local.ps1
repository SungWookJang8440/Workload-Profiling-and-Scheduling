# GPU Sharing Project - 원클릭 통합 실행 스크립트 (환경변수 갱신 대응 버전)
# 백엔드(Java/PostgreSQL)와 프론트엔드(React)를 자동으로 감지 및 실행합니다.

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  GPU Sharing Project - 로컬 통합 실행 프로세스 시작" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. 포트 확인 및 정리 (8000, 5173 포트가 사용 중이면 종료)
Write-Host "[1/3] 포트 중복 확인 중..." -ForegroundColor Yellow
$ports = @(8000, 5173)
foreach ($port in $ports) {
    # Find owning process
    $proc = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($proc) {
        $procId = $proc.OwningProcess
        Write-Host "-> 포트 $port`번이 이미 사용 중입니다 (PID: $procId). 기존 프로세스를 종료합니다..." -ForegroundColor Yellow
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

# 2. 백엔드 실행 준비 (경량 Maven 자동 다운로드)
Write-Host "[2/3] 백엔드(Spring Boot) 실행 준비 중..." -ForegroundColor Yellow
$mavenDir = "C:\Users\csu84\Desktop\Workspace\maven"
$mvnPath = "$mavenDir\apache-maven-3.9.6\bin\mvn.cmd"

if (-not (Test-Path $mvnPath)) {
    Write-Host "-> Maven이 존재하지 않아 경량 Maven을 다운로드합니다..." -ForegroundColor Cyan
    $zipPath = "C:\Users\csu84\Desktop\Workspace\maven.zip"
    
    # Download Apache Maven
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile $zipPath
    
    # Extract
    Expand-Archive -Path $zipPath -DestinationPath $mavenDir -Force
    Remove-Item -Path $zipPath -Force
    Write-Host "-> Maven 설정 완료!" -ForegroundColor Green
}

# 백엔드용 실행 스크립트 작성 (환경변수 강제 주입 포함)
$backendCmd = @'
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan
java -version
cd "C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\Project\Backend"
& "C:\Users\csu84\Desktop\Workspace\maven\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
'@

# 백엔드를 새로운 백그라운드 창에서 실행
Write-Host "-> 새로운 창에서 백엔드 서버를 시작합니다..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy Bypass", "-Command", $backendCmd -WindowStyle Normal

# 3. 프론트엔드 실행
Write-Host "[3/3] 프론트엔드(React) 실행 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 3 # 백엔드 기동 대기

# 프론트엔드를 새로운 백그라운드 창에서 실행
$frontendCmd = @'
cd "C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\Project\Frontend"
npm run dev
'@

Write-Host "-> 새로운 창에서 프론트엔드 서버를 시작합니다..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy Bypass", "-Command", $frontendCmd -WindowStyle Normal

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  실행 완료! 새로운 터미널 창 2개에서 각각 백엔드와 프론트엔드가 구동 중입니다." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "1. 잠시 후 브라우저가 열리면 http://localhost:5173 으로 접속해 주세요."
Write-Host "2. 실행을 종료하려면 열린 2개의 검은색 터미널 창을 닫아주시면 됩니다.`n"
