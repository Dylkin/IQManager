# TenderBot - Quick Start (PowerShell)
# Все значения вводятся одной строкой через запятую

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "TenderBot - Quick Start" -ForegroundColor Cyan
Write-Host "Bot: @DylkinIntegrationBot | Channel: 3855087918" -ForegroundColor Gray
Write-Host ""

# Быстрый ввод
if (-not $env:TELEGRAM_BOT_TOKEN) {
    $token = Read-Host "Bot Token (from @BotFather)"
} else { $token = $env:TELEGRAM_BOT_TOKEN }

if (-not $env:MAIL_USERNAME) {
    $email = Read-Host "Email (Gmail)"
} else { $email = $env:MAIL_USERNAME }

if (-not $env:MAIL_PASSWORD) {
    $pass = Read-Host "App Password" -AsSecureString
    $passPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($pass))
} else { $passPlain = $env:MAIL_PASSWORD }

Write-Host "Building..." -ForegroundColor Yellow
Set-Location ..\java-backend
mvn clean package -DskipTests -q

Write-Host "Starting... http://localhost:8080" -ForegroundColor Green
java -jar -Dserver.port=8080 target\tender-bot-1.0.0.jar `
  --telegram.bot.token=$token `
  --telegram.bot.username="@DylkinIntegrationBot" `
  --telegram.channel.id="3855087918" `
  --spring.mail.username=$email `
  --spring.mail.password=$passPlain
