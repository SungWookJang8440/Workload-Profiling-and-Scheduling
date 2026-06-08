# Windows Local Environment Setup Script for GPU Sharing Project
# This script will install Java 21 (Temurin JDK) and PostgreSQL 15, and configure the database.
# MUST BE RUN IN POWERSHELL AS ADMINISTRATOR.

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  GPU Sharing Project - 로컬 개발 환경 자동 설정 스크립트" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "이 스크립트는 JDK 21과 PostgreSQL 15를 자동으로 설치하고 설정합니다."
Write-Host "설정 중 윈도우 사용자 계정 컨트롤(UAC) 경고창이 나타나면 '예'를 클릭해주세요.`n"

# 1. JDK 21 설치
Write-Host "[1/4] Eclipse Temurin JDK 21 설치 중..." -ForegroundColor Yellow
try {
    # Install Temurin JDK 21 silently
    winget install --id EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
    Write-Host "-> JDK 21 설치 완료!" -ForegroundColor Green
} catch {
    Write-Host "-> JDK 21 설치 중 오류가 발생했거나 이미 설치되어 있습니다." -ForegroundColor Gray
}

# 2. PostgreSQL 15 설치
Write-Host "[2/4] PostgreSQL 15 설치 중 (Unattended Mode)..." -ForegroundColor Yellow
try {
    # Install PostgreSQL 15 silently and set postgres password to 'gpu_password'
    winget install --id PostgreSQL.PostgreSQL.15 --override "--mode unattended --superpassword gpu_password --serverport 5432" --silent --accept-package-agreements --accept-source-agreements
    Write-Host "-> PostgreSQL 15 설치 완료!" -ForegroundColor Green
} catch {
    Write-Host "-> PostgreSQL 15 설치 중 오류가 발생했거나 이미 설치되어 있습니다." -ForegroundColor Gray
}

# 3. 서비스 대기 및 환경 변수 새로고침
Write-Host "[3/4] PostgreSQL 서비스가 시작될 때까지 대기 중..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# 4. 데이터베이스 및 사용자 설정 생성
Write-Host "[4/4] 데이터베이스 및 사용자 계정 설정 중..." -ForegroundColor Yellow

$psqlPath = "C:\Program Files\PostgreSQL\15\bin\psql.exe"
if (-not (Test-Path $psqlPath)) {
    # Try finding any psql version
    $psqlPath = (Get-ChildItem -Path "C:\Program Files\PostgreSQL" -Filter "psql.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
}

if ($psqlPath) {
    Write-Host "Found psql at: $psqlPath" -ForegroundColor Gray
    $env:PGPASSWORD = "gpu_password"
    
    try {
        # Create user gpu_user
        & $psqlPath -U postgres -c "CREATE USER gpu_user WITH PASSWORD 'gpu_password';" 2>$null
        Write-Host "-> 데이터베이스 사용자(gpu_user) 생성 완료!" -ForegroundColor Green
    } catch {
        Write-Host "-> 사용자(gpu_user)가 이미 존재하거나 생성할 수 없습니다." -ForegroundColor Gray
    }

    try {
        # Create database gpu_sharing owned by gpu_user
        & $psqlPath -U postgres -c "CREATE DATABASE gpu_sharing OWNER gpu_user;" 2>$null
        Write-Host "-> 데이터베이스(gpu_sharing) 생성 완료!" -ForegroundColor Green
    } catch {
        Write-Host "-> 데이터베이스(gpu_sharing)가 이미 존재하거나 생성할 수 없습니다." -ForegroundColor Gray
    }
} else {
    Write-Host "[오류] psql.exe를 찾을 수 없습니다. PostgreSQL이 아직 설치 완료되지 않았거나 경로가 다릅니다." -ForegroundColor Red
    Write-Host "PostgreSQL 설치 마법사가 화면 뒤에 열려 있는지 확인하시고 완료된 후 다시 시도해 주세요." -ForegroundColor Yellow
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  로컬 인프라 설정 프로세스 완료!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "1. 터미널 창을 모두 닫고 새로 열어서 JDK 21 환경 변수를 갱신합니다."
Write-Host "2. 백엔드 폴더(Project/Backend)에서 아래 명령어를 실행하여 백엔드를 켭니다:"
Write-Host "   ./mvnw.cmd spring-boot:run   (또는 IDE에서 프로젝트 실행)"
Write-Host "3. 프론트엔드를 재시작하고 회원가입을 시도해 보세요!`n"
