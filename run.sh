#!/usr/bin/env bash
set -e

echo "========================================================"
echo "  NovaCalc - Advanced Scientific & AI Calculator"
echo "========================================================"
echo ""

JAR_PATH="target/NovaCalc.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "[INFO] Standalone JAR not found. Building with Maven..."
    mvn clean package -DskipTests
fi

echo "[INFO] Starting NovaCalc..."
java -jar "$JAR_PATH"
