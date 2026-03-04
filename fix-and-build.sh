#!/bin/bash

# Fix Maven to use JDK 21 instead of JDK 24

# Set JAVA_HOME to JDK 21
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# Verify the correct Java version
echo "Java version:"
java -version

echo ""
echo "Maven Java version:"
mvn -version

# Clean and rebuild
echo ""
echo "Cleaning project..."
./mvnw clean

echo ""
echo "Building project (skipping tests for speed)..."
./mvnw package -DskipTests

echo ""
echo "✅ Build complete! Now restart the application to see Swagger endpoints."

