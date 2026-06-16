# TenderBot - Updated Files

This archive contains only modified and new files.
Copy these files into your existing project to update it.

## Structure

```
updated-files/
├── java-backend/          # Spring Boot backend
│   ├── pom.xml            # Updated: Spring Boot 4.0.6, Java 25
│   └── src/main/java/com/tenderbot/
│       ├── entity/        # Config, ConfigGroup + updated entities
│       ├── repository/    # ConfigRepository + updated repos
│       ├── service/       # ConfigService, TelegramFetchService + updated services
│       ├── controller/    # ConfigController, TelegramController + updated controllers
│       ├── dto/           # ConfigDto + updated DTOs
│       ├── telegram/      # Updated TenderTelegramBot
│       ├── config/        # Updated DataSeeder, WebConfig
│       └── TenderBotApplication.java
│       └── src/main/resources/
│           └── application.properties
│
├── frontend/              # Angular 21 frontend
│   ├── angular.json
│   ├── tsconfig.json, tsconfig.app.json
│   ├── proxy.conf.json
│   ├── package.json
│   └── src/
│       ├── main.ts, styles.css, index.html
│       └── app/
│           ├── pages/
│           │   ├── config/          # NEW: Environment variables CRUD
│           │   ├── dashboard/       # UPDATED: Telegram fetch button
│           │   ├── tenders/
│           │   ├── tender-detail/
│           │   ├── suppliers/
│           │   └── logs/
│           ├── components/navbar/   # UPDATED: removed Settings link
│           ├── services/            # UPDATED: added telegram methods
│           ├── models/              # UPDATED: added Config, TelegramMessage
│           ├── app.component.ts
│           ├── app.config.ts
│           └── app.routes.ts        # UPDATED: removed Settings route
│
├── scripts/               # NEW: Startup scripts
│   ├── start.ps1
│   ├── start-quick.ps1
│   ├── start.bat
│   ├── start-with-config.bat
│   └── start.sh
│
└── docs/                  # Updated documentation
    ├── README.md
    ├── info.md
    ├── INSTALL.md
    └── .gitignore
```

## How to apply updates

### Option 1: Merge into existing project
```bash
# Copy java-backend files
cp -r updated-files/java-backend/* your-project/java-backend/

# Copy frontend files
cp -r updated-files/frontend/* your-project/frontend/

# Copy scripts
cp -r updated-files/scripts/* your-project/scripts/
```

### Option 2: Fresh clone + overwrite
1. Get base project structure
2. Copy all files from this archive into it
3. Build: `mvn clean package -DskipTests`

## Key changes summary

| Feature | Status |
|---------|--------|
| Java 25 + Spring Boot 4.0.6 | Updated |
| Angular 21 standalone | Updated |
| Config CRUD (env variables) | NEW |
| Telegram fetch (Dashboard button) | NEW |
| Settings page | REMOVED |
| Bot: @DylkinIntegrationBot | Preset |
| Channel ID: 3855087918 | Preset |
