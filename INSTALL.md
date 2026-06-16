# TenderBot — Установка на локальный компьютер

## Требования

| Компонент | Минимальная версия | Ссылка |
|-----------|-------------------|--------|
| **JDK** | 25 | [Oracle](https://www.oracle.com/java/technologies/downloads/#java25) / [Adoptium](https://adoptium.net/temurin/releases/?version=25) |
| **Maven** | 3.9+ | [maven.apache.org](https://maven.apache.org/download.cgi) |
| **Node.js** | 20+ (только для разработки frontend) | [nodejs.org](https://nodejs.org/) |

## Быстрый старт (Windows)

### Вариант 1: Через `start-with-config.bat`

1. Откройте папку `scripts/`
2. Запустите `start-with-config.bat`
3. Введите запрашиваемые данные:
   - **Bot Token** — получите у [@BotFather](https://t.me/BotFather)
   - **Email** — Gmail (или другой SMTP)
   - **App Password** — [как получить](#как-получить-app-password)
4. Скрипт автоматически соберёт и запустит проект
5. Откройте браузер: **http://localhost:8080**

### Вариант 2: Через `start.bat` (с переменными окружения)

```cmd
set TELEGRAM_BOT_TOKEN=ваш_токен
set TELEGRAM_BOT_USERNAME=@DylkinIntegrationBot
set TELEGRAM_CHANNEL_ID=3855087918
set MAIL_USERNAME=ваш_email@gmail.com
set MAIL_PASSWORD=ваш_app_password

cd scripts
start.bat
```

## Быстрый старт (Linux / Mac)

```bash
cd scripts
chmod +x start.sh
./start.sh
```

Или с переменными:

```bash
export TELEGRAM_BOT_TOKEN="ваш_токен"
export TELEGRAM_BOT_USERNAME="@DylkinIntegrationBot"
export TELEGRAM_CHANNEL_ID="3855087918"
export MAIL_USERNAME="ваш_email@gmail.com"
export MAIL_PASSWORD="ваш_app_password"

./scripts/start.sh
```

## Ручная сборка

### 1. Backend

```bash
cd java-backend
mvn clean package -DskipTests
java -jar target/tender-bot-1.0.0.jar \
  --telegram.bot.token=ВАШ_ТОКЕН \
  --telegram.bot.username=@DylkinIntegrationBot \
  --telegram.channel.id=3855087918 \
  --spring.mail.username=EMAIL@gmail.com \
  --spring.mail.password=APP_PASSWORD
```

### 2. Frontend (только для разработки)

```bash
cd frontend
npm install
ng serve --proxy-config proxy.conf.json
# Откроется на http://localhost:4200
```

## Настройки по умолчанию

| Параметр | Значение | Где редактировать |
|----------|----------|-------------------|
| `telegram.bot.username` | `@DylkinIntegrationBot` | Страница "Переменные окружения" |
| `telegram.channel.id` | `3855087918` | Страница "Переменные окружения" |
| `telegram.bot.token` | *(пусто)* | Страница "Переменные окружения" или переменная окружения |
| `spring.mail.host` | `smtp.gmail.com` | Страница "Переменные окружения" |
| `spring.mail.port` | `587` | Страница "Переменные окружения" |
| `spring.mail.username` | *(пусто)* | Страница "Переменные окружения" |
| `spring.mail.password` | *(пусто)* | Страница "Переменные окружения" |

## Как получить App Password

1. Откройте [Google Account](https://myaccount.google.com/)
2. Перейдите в **Security** → **2-Step Verification** (включите)
3. Вернитесь в **Security** → **App passwords**
4. Выберите: **Select app** → **Mail** → **Generate**
5. Скопируйте 16-символьный пароль (например: `abcd efgh ijkl mnop`)

## Как получить Telegram Bot Token

1. Найдите [@BotFather](https://t.me/BotFather) в Telegram
2. Отправьте `/newbot`
3. Введите имя бота: `DylkinIntegrationBot`
4. Получите токен вида: `123456789:ABCdefGHIjklMNOpqrsTUVwxyz`
5. Добавьте бота в канал **3855087918** администратором

## Структура проекта

```
TenderBot/
├── java-backend/              # Spring Boot (Java 25)
│   ├── pom.xml
│   ├── src/main/java/...      # Исходный код
│   └── src/main/resources/
│       ├── application.properties
│       └── static/            # Angular build
├── frontend/                  # Angular 21
│   ├── src/app/
│   │   ├── pages/             # Страницы (dashboard, tenders, config, ...)
│   │   ├── components/        # Компоненты (navbar)
│   │   ├── services/          # HTTP клиент
│   │   └── models/            # TypeScript интерфейсы
│   └── angular.json
├── scripts/                   # Скрипты запуска
│   ├── start.bat              # Windows (с переменными окружения)
│   ├── start-with-config.bat  # Windows (с вводом данных)
│   └── start.sh               # Linux/Mac
├── README.md                  # Описание проекта
├── info.md                    # Структура проекта
└── INSTALL.md                 # Эта инструкция
```

## Веб-интерфейс

После запуска откройте **http://localhost:8080**

| Страница | Описание |
|----------|----------|
| **Панель управления** | Статистика, кнопка "Загрузить тендеры из Telegram", последние действия |
| **Тендеры** | Список всех тендеров с поиском и фильтрацией |
| **Поставщики** | CRUD управление поставщиками (redhon.ru, dia-m.ru) |
| **Журнал** | Полный лог действий с поиском |
| **Переменные окружения** | Полный CRUD всех настроек системы |

## Порт

По умолчанию: **8080**

Чтобы изменить, добавьте: `-Dserver.port=8081`

## Логи

Windows: `java-backend/logs/tenderbot.log`
Linux/Mac: `/tmp/tenderbot/logs/tenderbot.log`
