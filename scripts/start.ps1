# ==========================================
#    TenderBot - PowerShell Startup
#    Java 25 + Spring Boot 4.0.6
# ==========================================

# Установка кодировки UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   TenderBot - Local Startup" -ForegroundColor Cyan
Write-Host "   Java 25 + Spring Boot 4.0.6" -ForegroundColor Cyan
Write-Host "   Bot: @DylkinIntegrationBot" -ForegroundColor Cyan
Write-Host "   Channel: 3855087918" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# --- 1. Проверка Java ---
Write-Host "[1/5] Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Out-String
    if ($javaVersion -match '"25\.') {
        Write-Host "[OK] Java 25 found." -ForegroundColor Green
    } else {
        Write-Host "[!] Java found but version may not be 25." -ForegroundColor Yellow
    }
    Write-Host $javaVersion -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] Java not found!" -ForegroundColor Red
    Write-Host "Install JDK 25: https://www.oracle.com/java/technologies/downloads/#java25" -ForegroundColor Yellow
    Write-Host "Then add JAVA_HOME to environment variables." -ForegroundColor Yellow
    Pause
    exit 1
}

# --- 2. Проверка Maven ---
Write-Host "[2/5] Checking Maven..." -ForegroundColor Yellow
try {
    $mvnVersion = mvn -version 2>&1 | Select-Object -First 1
    Write-Host "[OK] $mvnVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Maven not found!" -ForegroundColor Red
    Write-Host "Install Maven 3.9+: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    Write-Host "Add to PATH: C:\apache-maven-3.9.x\bin" -ForegroundColor Yellow
    Pause
    exit 1
}

# --- 3. Ввод настроек (если не заданы) ---
Write-Host "[3/5] Configuration..." -ForegroundColor Yellow

# Предустановленные значения
$Global:TELEGRAM_BOT_USERNAME = "@DylkinIntegrationBot"
$Global:TELEGRAM_CHANNEL_ID = "3855087918"

# Bot Token
if (-not $env:TELEGRAM_BOT_TOKEN) {
    Write-Host ""
    Write-Host "Get your Bot Token from @BotFather in Telegram" -ForegroundColor Cyan
    $Global:TELEGRAM_BOT_TOKEN = Read-Host "Enter Telegram Bot Token"
} else {
    $Global:TELEGRAM_BOT_TOKEN = $env:TELEGRAM_BOT_TOKEN
    Write-Host "[OK] Token from environment variable" -ForegroundColor Green
}

# Email
if (-not $env:MAIL_USERNAME) {
    Write-Host ""
    $Global:MAIL_USERNAME = Read-Host "Enter Email (Gmail recommended)"
} else {
    $Global:MAIL_USERNAME = $env:MAIL_USERNAME
    Write-Host "[OK] Email from environment variable" -ForegroundColor Green
}

# App Password
if (-not $env:MAIL_PASSWORD) {
    Write-Host ""
    Write-Host "For Gmail: Generate App Password at https://myaccount.google.com/apppasswords" -ForegroundColor Cyan
    $Global:MAIL_PASSWORD = Read-Host "Enter App Password" -AsSecureString
    $Global:MAIL_PASSWORD_PLAIN = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($Global:MAIL_PASSWORD))
} else {
    $Global:MAIL_PASSWORD_PLAIN = $env:MAIL_PASSWORD
    Write-Host "[OK] Password from environment variable" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] Configuration:" -ForegroundColor Green
Write-Host "  Bot: $Global:TELEGRAM_BOT_USERNAME" -ForegroundColor Gray
Write-Host "  Channel: $Global:TELEGRAM_CHANNEL_ID" -ForegroundColor Gray
Write-Host "  Email: $Global:MAIL_USERNAME" -ForegroundColor Gray
Write-Host ""

# --- 4. Сборка ---
Write-Host "[4/5] Building project..." -ForegroundColor Yellow
Set-Location ..\java-backend
$mvnResult = mvn clean package -DskipTests 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Build failed!" -ForegroundColor Red
    $mvnResult | Select-Object -Last 20 | Write-Host -ForegroundColor Red
    Pause
    exit 1
}
Write-Host "[OK] Build successful!" -ForegroundColor Green
Write-Host ""

# --- 5. Запуск ---
Write-Host "[5/5] Starting TenderBot..." -ForegroundColor Green
Write-Host "Open browser: http://localhost:8080" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

java -jar -Dserver.port=8080 target\tender-bot-1.0.0.jar `
  --telegram.bot.token=$Global:TELEGRAM_BOT_TOKEN `
  --telegram.bot.username=$Global:TELEGRAM_BOT_USERNAME `
  --telegram.channel.id=$Global:TELEGRAM_CHANNEL_ID `
  --spring.mail.username=$Global:MAIL_USERNAME `
  --spring.mail.password=$Global:MAIL_PASSWORD_PLAIN
