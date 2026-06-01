#!/bin/bash

# GPU Sharing Jang - Spring Boot Backend Start Script

echo "Starting GPU Sharing Jang Backend..."

cd ../Backend || { echo "Backend directory not found"; exit 1; }

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp .env.example .env
    echo "Please edit .env file with your configuration before running again."
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Set JAVA_HOME for Java 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Upgrade Maven to latest version
echo "Upgrading Maven..."
sudo apt update
sudo apt install -y maven

# Build project
echo "Building project with upgraded Maven..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed. Please check the errors above."
    exit 1
fi

# Run the application
echo "Starting the application..."
java -jar target/sharing-jang-1.0.0.jar
