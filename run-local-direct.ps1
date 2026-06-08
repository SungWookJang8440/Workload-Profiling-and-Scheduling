# GPU Sharing Project - 로컬 직접 통합 실행 스크립트
# 별도의 창을 띄우지 않고, 백그라운드 프로세스로 구동하며 로그를 파일로 기록합니다.

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  GPU Sharing Project - 로컬 백그라운드 통합 실행" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. 포트 확인 및 정리 (8000, 5173 포트가 사용 중이면 종료)
Write-Host "[1/3] 포트 중복 확인 및 프로세스 종료 중..." -ForegroundColor Yellow
$ports = @(8000, 5173)
foreach ($port in $ports) {
    $proc = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($proc) {
        $procId = $proc.OwningProcess
        Write-Host "-> 포트 $port`번이 이미 사용 중입니다 (PID: $procId). 기존 프로세스를 종료합니다..." -ForegroundColor Yellow
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

# 2. 백엔드 실행 및 로그 파일 리다이렉션
Write-Host "[2/3] 백엔드(Spring Boot) 백그라운드 구동 중..." -ForegroundColor Yellow
$backendLog = "C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\backend.log"
$mavenDir = "C:\Users\csu84\Desktop\Workspace\maven"
$mvnPath = "$mavenDir\apache-maven-3.9.6\bin\mvn.cmd"

# 환경변수 설정 및 백엔드 백그라운드 실행
$backendScript = @"
`$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
`$env:Path = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin;' + `$env:Path
cd 'C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\Project\Backend'
& '$mvnPath' spring-boot:run > '$backendLog' 2>&1
"@

# 백그라운드로 실행되도록 -WindowStyle Hidden으로 PowerShell 백그라운드 잡 실행
Start-Process powershell -ArgumentList "-ExecutionPolicy", "Bypass", "-Command", $backendScript -WindowStyle Hidden
Write-Host "-> 백엔드가 시작되었습니다. 로그 파일: backend.log" -ForegroundColor Green

# 3. 프론트엔드 실행 및 로그 파일 리다이렉션
Write-Host "[3/3] 프론트엔드(React) 백그라운드 구동 중..." -ForegroundColor Yellow
$frontendLog = "C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\frontend.log"

$frontendScript = @"
cd 'C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\Project\Frontend'
npm run dev > '$frontendLog' 2>&1
"@

Start-Process powershell -ArgumentList "-ExecutionPolicy", "Bypass", "-Command", $frontendScript -WindowStyle Hidden
Write-Host "-> 프론트엔드가 시작되었습니다. 로그 파일: frontend.log" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  실행 프로세스 설정 완료!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "실시간 로그를 확인하고 싶다면 아래 명령어를 사용하세요:"
Write-Host "  * 백엔드 로그 확인: Get-Content -Wait -Tail 20 .\backend.log"
Write-Host "  * 프론트엔드 로그 확인: Get-Content -Wait -Tail 20 .\frontend.log"
Write-Host "  (또는 VS Code에서 해당 log 파일을 열어 바로 보실 수 있습니다.)`n"
