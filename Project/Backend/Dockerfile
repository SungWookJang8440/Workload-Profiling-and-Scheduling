# Multi-stage build for Spring Boot
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# pom.xml 복사 및 의존성 다운로드
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 소스 코드 복사
COPY src src

# 빌드
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/sharing-jang-1.0.0.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
