@echo off
setlocal
title NovaCalc - Launching...

echo ========================================================
echo   NovaCalc - Advanced Scientific & AI Calculator
echo ========================================================
echo.

set JAR_PATH=target\NovaCalc.jar

if not exist "%JAR_PATH%" (
    echo [INFO] Standalone JAR not found. Building with Maven...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo [ERROR] Build failed. Please verify that Java (JDK 17+) and Maven are installed.
        pause
        exit /b 1
    )
)

echo [INFO] Starting NovaCalc...
java -jar "%JAR_PATH%"

if errorlevel 1 (
    echo.
    echo [ERROR] NovaCalc exited with an error code.
    pause
)

endlocal
