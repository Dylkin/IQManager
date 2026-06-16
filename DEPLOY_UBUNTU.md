# Развертывание IQManager на Ubuntu Server

Пошаговая инструкция по развертыванию приложения TenderBot/IQManager на сервере с Ubuntu 22.04/24.04 LTS.

---

## Требования

| Компонент | Версия | Назначение |
|-----------|--------|------------|
| Ubuntu | 22.04 LTS или новее | Операционная система |
| JDK | 25 | Запуск Spring Boot backend |
| Maven | 3.9+ | Сборка backend |
| Node.js | 22+ | Сборка Angular frontend |
| Git | любая | Клонирование репозитория |
| (опционально) Nginx | любая | Обратный прокси и SSL |

---

## 1. Подготовка сервера

### Обновление системы

```bash
sudo apt update && sudo apt upgrade -y
```

### Установка базовых пакетов

```bash
sudo apt install -y curl wget git unzip software-properties-common
```

---

## 2. Установка Java 25

Рекомендуется использовать Eclipse Temurin (Adoptium).

```bash
# Добавление репозитория Adoptium
sudo apt install -y wget apt-transport-https
sudo mkdir -p /etc/apt/keyrings
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/keyrings/adoptium.asc

echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt update
sudo apt install -y temurin-25-jdk
```

Проверка:

```bash
java -version
```

Должно отобразиться что-то вроде:

```
openjdk version "25.0.3" 2026-04-21 LTS
```

---

## 3. Установка Maven 3.9+

```bash
sudo apt install -y maven
```

Проверка:

```bash
mvn -version
```

Если в репозитории Ubuntu старая версия Maven, установите вручную:

```bash
MAVEN_VERSION=3.9.11
wget https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
sudo tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt
sudo ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven

echo 'export M2_HOME=/opt/maven' | sudo tee /etc/profile.d/maven.sh
echo 'export PATH=${M2_HOME}/bin:${PATH}' | sudo tee -a /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
mvn -version
```

---

## 4. Установка Node.js 22+

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

Проверка:

```bash
node -v
npm -v
```

Установите Angular CLI глобально:

```bash
sudo npm install -g @angular/cli
```

---

## 5. Клонирование репозитория

```bash
cd /opt
sudo git clone https://github.com/Dylkin/IQManager.git
sudo chown -R $USER:$USER /opt/IQManager
cd /opt/IQManager
```

---

## 6. Сборка frontend

```bash
cd /opt/IQManager/frontend
npm install
ng build --configuration production
```

Результат сборки по умолчанию находится в `/tmp/angular-dist/browser/`.

Скопируйте собранный frontend в static-ресурсы backend:

```bash
mkdir -p /opt/IQManager/java-backend/src/main/resources/static
cp -r /tmp/angular-dist/browser/* /opt/IQManager/java-backend/src/main/resources/static/
```

---

## 7. Сборка backend

```bash
cd /opt/IQManager/java-backend
mvn clean package -DskipTests
```

После успешной сборки появится файл:

```
/opt/IQManager/java-backend/target/tender-bot-1.0.0.jar
```

---

## 8. Конфигурация приложения

Перед запуском необходимо задать обязательные параметры. Их можно передать через переменные окружения или аргументы командной строки.

### Обязательные параметры

| Переменная | Описание |
|------------|----------|
| `TELEGRAM_BOT_TOKEN` | Токен бота от [@BotFather](https://t.me/BotFather) |
| `TELEGRAM_BOT_USERNAME` | Имя бота, например `@DylkinIntegrationBot` |
| `TELEGRAM_CHANNEL_ID` | ID канала Telegram для мониторинга |
| `MAIL_USERNAME` | Email для отправки писем |
| `MAIL_PASSWORD` | App Password почтового ящика |

### Пример запуска вручную

```bash
cd /opt/IQManager/java-backend
export TELEGRAM_BOT_TOKEN="your_bot_token_here"
export TELEGRAM_BOT_USERNAME="@DylkinIntegrationBot"
export TELEGRAM_CHANNEL_ID="3855087918"
export MAIL_USERNAME="your_email@gmail.com"
export MAIL_PASSWORD="your_app_password"

java -jar -Dserver.port=8080 target/tender-bot-1.0.0.jar \
  --telegram.bot.token="${TELEGRAM_BOT_TOKEN}" \
  --telegram.bot.username="${TELEGRAM_BOT_USERNAME}" \
  --telegram.channel.id="${TELEGRAM_CHANNEL_ID}" \
  --spring.mail.username="${MAIL_USERNAME}" \
  --spring.mail.password="${MAIL_PASSWORD}"
```

Приложение будет доступно по адресу:

```
http://your-server-ip:8080
```

---

## 9. Запуск как systemd-сервис (рекомендуется)

Создайте файл сервиса:

```bash
sudo tee /etc/systemd/system/iqmanager.service > /dev/null <<EOF
[Unit]
Description=IQManager TenderBot
After=network.target

[Service]
Type=simple
User=iqmanager
Group=iqmanager
WorkingDirectory=/opt/IQManager/java-backend
Environment="TELEGRAM_BOT_TOKEN=your_bot_token_here"
Environment="TELEGRAM_BOT_USERNAME=@DylkinIntegrationBot"
Environment="TELEGRAM_CHANNEL_ID=3855087918"
Environment="MAIL_USERNAME=your_email@gmail.com"
Environment="MAIL_PASSWORD=your_app_password"
Environment="JWT_SECRET=your-strong-secret-key-min-32-chars-long"
ExecStart=/usr/bin/java -jar -Dserver.port=8080 target/tender-bot-1.0.0.jar \
  --telegram.bot.token=\${TELEGRAM_BOT_TOKEN} \
  --telegram.bot.username=\${TELEGRAM_BOT_USERNAME} \
  --telegram.channel.id=\${TELEGRAM_CHANNEL_ID} \
  --spring.mail.username=\${MAIL_USERNAME} \
  --spring.mail.password=\${MAIL_PASSWORD}
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
```

Создайте пользователя и дайте права:

```bash
sudo useradd -r -s /bin/false iqmanager
sudo chown -R iqmanager:iqmanager /opt/IQManager
```

Перезагрузите systemd и запустите сервис:

```bash
sudo systemctl daemon-reload
sudo systemctl enable iqmanager
sudo systemctl start iqmanager
```

Проверка статуса:

```bash
sudo systemctl status iqmanager
```

Просмотр логов:

```bash
sudo journalctl -u iqmanager -f
```

---

## 10. Настройка Nginx (опционально)

Установите Nginx:

```bash
sudo apt install -y nginx
```

Создайте конфиг:

```bash
sudo tee /etc/nginx/sites-available/iqmanager > /dev/null <<EOF
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }
}
EOF
```

Активируйте сайт:

```bash
sudo ln -s /etc/nginx/sites-available/iqmanager /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### SSL через Certbot (рекомендуется)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

---

## 11. Настройка firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
# если Nginx не используется, откройте порт приложения:
# sudo ufw allow 8080/tcp
sudo ufw enable
```

---

## 12. Обновление приложения

Для обновления до новой версии из репозитория:

```bash
cd /opt/IQManager
sudo systemctl stop iqmanager
git pull origin main

cd frontend
npm install
ng build --configuration production
cp -r /tmp/angular-dist/browser/* /opt/IQManager/java-backend/src/main/resources/static/

cd ../java-backend
mvn clean package -DskipTests

sudo systemctl start iqmanager
```

---

## 13. Проверка работоспособности

1. Откройте в браузере `http://your-server-ip:8080` (или ваш домен).
2. Войдите с дефолтными учётными данными:
   - Email: `pavel.dylkin@gmail.com`
   - Password: `00016346`
3. Перейдите в раздел **«Переменные окружения»** и убедитесь, что параметры Telegram и Email корректны.
4. Нажмите **«Загрузить тендеры из Telegram»** и проверьте, что сообщения загружаются.
5. Для сообщений со статусом `NEW` нажмите **«Создать тендер»** — через несколько секунд тендер должен появиться в списке.

---

## Возможные проблемы

### Приложение не запускается

Проверьте логи:

```bash
sudo journalctl -u iqmanager -f
```

### Ошибка `JAVA_HOME not set`

Установите переменную окружения:

```bash
sudo tee /etc/profile.d/java.sh > /dev/null <<EOF
export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64
export PATH=\$JAVA_HOME/bin:\$PATH
EOF
source /etc/profile.d/java.sh
```

### Telegram не загружает сообщения

- Проверьте, что бот добавлен в канал администратором.
- Убедитесь, что `TELEGRAM_CHANNEL_ID` указан корректно (можно с префиксом `-100` для супергрупп).
- Проверьте валидность токена через запрос: `https://api.telegram.org/bot<TOKEN>/getMe`.

### Email не отправляется

- Для Gmail используйте **App Password**, а не обычный пароль.
- Проверьте настройки SMTP (`MAIL_HOST`, `MAIL_PORT`).
