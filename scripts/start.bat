@echo off
chcp 65001 >nul
title TenderBot

echo ==========================================
echo    TenderBot - Local Startup
echo    Java 25 + Spring Boot 4.0.6
echo ==========================================
echo.

REM Check Java
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found. Please install JDK 25.
    echo Download: https://www.oracle.com/java/technologies/downloads/#java25
    pause
    exit /b 1
)

echo [OK] Java found.
java -version
echo.

REM Check Maven
mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven not found. Please install Maven 3.9+
    echo Download: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo [OK] Maven found.
echo.

REM Build
echo [1/3] Building project...
cd ..\java-backend
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)
echo [OK] Build successful.
echo.

REM Start
echo [2/3] Starting TenderBot...
echo [3/3] Open browser: http://localhost:8080
echo.
echo Press Ctrl+C to stop.
echo.

java -jar -Dserver.port=8080 target\tender-bot-1.0.0.jar ^
  --telegram.bot.token=%TELEGRAM_BOT_TOKEN% ^
  --telegram.bot.username=%TELEGRAM_BOT_USERNAME% ^
  --telegram.channel.id=%TELEGRAM_CHANNEL_ID% ^
  --spring.mail.username=%MAIL_USERNAME% ^
  --spring.mail.password=%MAIL_PASSWORD%

pause
