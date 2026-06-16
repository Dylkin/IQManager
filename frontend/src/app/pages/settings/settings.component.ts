import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2 class="fw-bold mb-4">Настройки</h2>

    <div *ngIf="saved" class="alert alert-success alert-dismissible fade show" role="alert">
      Настройки сохранены!
      <button type="button" class="btn-close" (click)="saved = false"></button>
    </div>

    <div class="alert alert-warning d-flex align-items-center gap-2">
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
      <span class="small">Эти настройки загружаются из переменных окружения при запуске. Для изменения необходимо перезапустить приложение.</span>
    </div>

    <div class="row g-4">
      <div class="col-lg-6">
        <div class="card">
          <div class="card-header bg-white">
            <h5 class="mb-0 fw-semibold d-flex align-items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              Настройки Telegram
            </h5>
          </div>
          <div class="card-body">
            <div class="mb-3">
              <label class="form-label">Bot Token</label>
              <input type="password" class="form-control" [(ngModel)]="config.telegramBotToken" placeholder="Введите токен бота">
            </div>
            <div class="mb-3">
              <label class="form-label">Bot Username</label>
              <input type="text" class="form-control" [(ngModel)]="config.telegramBotUsername" placeholder="@your_bot">
            </div>
            <div class="mb-3">
              <label class="form-label">ID канала</label>
              <input type="text" class="form-control" [(ngModel)]="config.telegramChannelId" placeholder="-1001234567890">
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card">
          <div class="card-header bg-white">
            <h5 class="mb-0 fw-semibold d-flex align-items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#a855f7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
              Настройки Email
            </h5>
          </div>
          <div class="card-body">
            <div class="mb-3">
              <label class="form-label">SMTP сервер</label>
              <input type="text" class="form-control" [(ngModel)]="config.mailHost" placeholder="smtp.gmail.com">
            </div>
            <div class="mb-3">
              <label class="form-label">Порт</label>
              <input type="text" class="form-control" [(ngModel)]="config.mailPort" placeholder="587">
            </div>
            <div class="mb-3">
              <label class="form-label">Email отправителя</label>
              <input type="email" class="form-control" [(ngModel)]="config.mailUsername" placeholder="your@email.com">
            </div>
            <div class="mb-3">
              <label class="form-label">Пароль / App Password</label>
              <input type="password" class="form-control" [(ngModel)]="config.mailPassword" placeholder="••••••••">
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card">
          <div class="card-header bg-white">
            <h5 class="mb-0 fw-semibold d-flex align-items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>
              Настройки парсера
            </h5>
          </div>
          <div class="card-body">
            <div class="mb-3">
              <label class="form-label">Интервал опроса Telegram (сек)</label>
              <input type="number" class="form-control" [(ngModel)]="config.pollInterval">
            </div>
            <div class="mb-3">
              <label class="form-label">Интервал обработки тендеров (сек)</label>
              <input type="number" class="form-control" [(ngModel)]="config.processInterval">
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-12">
        <button class="btn btn-primary btn-lg w-100" (click)="save()">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
          Сохранить настройки
        </button>
      </div>
    </div>
  `,
})
export class SettingsComponent {
  saved = false;
  config = {
    telegramBotToken: '',
    telegramBotUsername: '',
    telegramChannelId: '',
    mailHost: 'smtp.gmail.com',
    mailPort: '587',
    mailUsername: '',
    mailPassword: '',
    pollInterval: '60',
    processInterval: '120',
  };

  save() {
    this.saved = true;
    setTimeout(() => this.saved = false, 3000);
  }
}
