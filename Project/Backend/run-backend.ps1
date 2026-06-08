# 로컬 백엔드 실행 스크립트
$ErrorActionPreference = "Stop"

# Java 21 환경 변수 강제 주입
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

Write-Host "JAVA_HOME: $env:JAVA_HOME"
java -version

# Maven으로 스프링부트 빌드 및 구동
cd "C:\Users\csu84\Desktop\Workspace\Workload-Profiling-and-Scheduling\Project\Backend"
& "C:\Users\csu84\Desktop\Workspace\maven\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
