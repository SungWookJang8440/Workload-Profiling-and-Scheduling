#!/bin/bash

# Java 21 + Maven Setup Script for WSL2

echo "=== Setting up Java 21 and Maven for GPU Sharing Jang ==="

# Update package list
echo "Updating package list..."
sudo apt update

# Install Java 21 JDK (full version with compiler)
echo "Installing OpenJDK 21..."
sudo apt install -y openjdk-21-jdk

# Install latest Maven
echo "Installing latest Maven..."
sudo apt install -y maven

# Set Java 21 as default
echo "Setting Java 21 as default..."
sudo update-alternatives --config java
sudo update-alternatives --config javac

# Verify installations
echo "=== Verification ==="
echo "Java version:"
java -version
echo ""
echo "Javac version:"
javac -version
echo ""
echo "Maven version:"
mvn -version

# Set environment variables for current session
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Add to .bashrc for persistence
echo "Adding environment variables to .bashrc..."
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

echo "=== Setup Complete ==="
echo "Please run 'source ~/.bashrc' or restart your terminal to apply changes permanently."
echo "Then run './start.sh' to build and run the application."
