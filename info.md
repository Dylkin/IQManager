TenderBot - Automated Tender Processing System

Stack:
- Frontend: Angular 21 (standalone components), Bootstrap 5.3, TypeScript 5.9
- Backend: Java 25, Spring Boot 4.0.6
- Database: H2 (file-based)
- Parsing: JSoup 1.17.2
- Documents: Apache PDFBox 3.0.2, Apache POI 5.2.5
- Telegram: TelegramBots 6.9.7.1
- Email: Spring Mail (JavaMail)
- Build: Maven

Angular Setup:
- Generated with Angular CLI 21.2.12
- Standalone components (no NgModules)
- Lazy-loaded routes with loadComponent
- Bootstrap 5.3 for UI styling
- HTTP client with proxy to backend

Frontend Structure:
  src/app/components/
    navbar/              Sidebar navigation component
  src/app/pages/
    dashboard/           Dashboard with statistics cards
    tenders/             Tender list with search/filter
    tender-detail/       Tender lots + processing logs
    suppliers/           Supplier CRUD management
    logs/                Full action log with search
    config/              Environment variables CRUD (group filter, secrets, read-only)
    settings/            System configuration
  src/app/services/
    tender.service.ts    HTTP client for REST API
  src/app/models/
    models.ts            TypeScript interfaces (Tender, TenderItem, LogEntry, etc.)
  src/app/
    app.component.ts     Root component with layout
    app.config.ts        Angular providers (Router, HttpClient)
    app.routes.ts        Lazy-loaded route definitions
  src/
    main.ts              bootstrapApplication entry point
    index.html
    styles.css           Global styles + Bootstrap CSS

Backend Structure:
  com.tenderbot/
    config/              WebConfig (CORS), DataSeeder
    entity/              JPA entities (Tender, TenderItem, Supplier, Config, etc.)
    repository/          Spring Data JPA repositories (including ConfigRepository)
    service/             Business logic (parser, search, email, logging, config CRUD)
    telegram/            Telegram bot integration
    controller/          REST API controllers (including ConfigController) + SPA forwarding
    dto/                 Data transfer objects (including ConfigDto)

Usage:
  ng serve              Start Angular dev server with proxy
  ng build              Production build (output to /tmp/angular-dist/browser/)
  mvn spring-boot:run   Start Spring Boot backend
  mvn package           Build executable JAR with embedded Angular

Key Features:
  - Telegram channel monitoring for tender links
  - Automatic parsing of goszakupki.by tender pages
  - Product search on supplier websites (redhon.ru, dia-m.ru)
  - Document download and analysis (PDF, DOC, XLS)
  - Automated email quote requests to suppliers
  - Full audit logging through web interface
  - Environment variables CRUD via web UI (grouped, searchable, secret masking)
  - Auto-refreshing dashboard with real-time statistics
