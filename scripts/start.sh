#!/bin/bash

echo "=========================================="
echo "   TenderBot - Local Startup"
echo "   Java 25 + Spring Boot 4.0.6"
echo "=========================================="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found. Please install JDK 25."
    echo "Download: https://www.oracle.com/java/technologies/downloads/#java25"
    exit 1
fi

echo "[OK] Java found:"
java -version
echo ""

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven not found. Please install Maven 3.9+"
    echo "Download: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "[OK] Maven found."
echo ""

# Build
echo "[1/3] Building project..."
cd ../java-backend || exit 1
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "[ERROR] Build failed!"
    exit 1
fi
echo "[OK] Build successful."
echo ""

# Start
echo "[2/3] Starting TenderBot..."
echo "[3/3] Open browser: http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop."
echo ""

java -jar -Dserver.port=8080 target/tender-bot-1.0.0.jar \
  --telegram.bot.token="${TELEGRAM_BOT_TOKEN:-test}" \
  --telegram.bot.username="${TELEGRAM_BOT_USERNAME:-@DylkinIntegrationBot}" \
  --telegram.channel.id="${TELEGRAM_CHANNEL_ID:-3855087918}" \
  --spring.mail.username="${MAIL_USERNAME:-}" \
  --spring.mail.password="${MAIL_PASSWORD:-}"
