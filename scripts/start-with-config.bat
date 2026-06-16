@echo off
chcp 65001 >nul
title TenderBot - DylkinIntegrationBot

echo ==========================================
echo    TenderBot - DylkinIntegrationBot
echo    Channel ID: 3855087918
echo ==========================================
echo.

REM Set config
set TELEGRAM_BOT_USERNAME=@DylkinIntegrationBot
set TELEGRAM_CHANNEL_ID=3855087918

REM Prompt for token
echo Please enter your Telegram Bot Token:
echo (Get it from @BotFather)
set /p TELEGRAM_BOT_TOKEN="Token: "

echo.
echo Please enter your Email (Gmail recommended):
set /p MAIL_USERNAME="Email: "
echo.
echo Please enter your App Password:
set /p MAIL_PASSWORD="App Password: "

echo.
echo [OK] Configuration set.
echo Bot: %TELEGRAM_BOT_USERNAME%
echo Channel: %TELEGRAM_CHANNEL_ID%
echo.

REM Check Java
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found. Please install JDK 25.
    pause
    exit /b 1
)

REM Check Maven
mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven not found. Please install Maven 3.9+
    pause
    exit /b 1
)

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
