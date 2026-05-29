#!/bin/bash

echo "=== Debug Spring Boot Application ==="

cd ../Backend || { echo "Backend directory not found"; exit 1; }

# Check if port 8000 is in use
echo "Checking port 8000 usage..."
netstat -tlnp | grep 8000

# Kill any existing Java processes on port 8000
echo "Killing existing Java processes..."
pkill -f "java.*sharing-jang"

# Start Spring Boot with debug
echo "Starting Spring Boot in debug mode..."
export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"
java -jar target/sharing-jang-1.0.0.jar --server.port=8000

echo "=== Debug session started ==="
echo "Connect your IDE to localhost:5005 for remote debugging"
