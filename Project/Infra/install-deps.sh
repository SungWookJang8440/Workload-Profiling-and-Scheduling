#!/bin/bash

echo "Installing dependencies for GPU Sharing Jang project..."

# Update package list
sudo apt update

# Install Java 17 JDK
echo "Installing OpenJDK 17..."
sudo apt install -y openjdk-17-jdk

# Install Maven
echo "Installing Maven..."
sudo apt install -y maven

# Verify installations
echo "=== Verification ==="
echo "Java version:"
java -version
echo ""
echo "Maven version:"
mvn -version

echo "Installation completed!"
