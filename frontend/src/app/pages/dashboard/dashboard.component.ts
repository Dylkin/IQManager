import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TenderService } from '../../services/tender.service';
import { PlainNumberPipe } from '../../pipes/plain-number.pipe';
import { DashboardStats, LogEntry, TelegramMessage, Tender, STATUS_CLASSES, STATUS_LABELS } from '../../models/models';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, PlainNumberPipe],
  template: `
    <div class="mb-4 d-flex align-items-center justify-content-between">
      <h2 class="fw-bold">Тендеры IQManager</h2>
      <button class="btn btn-primary" (click)="fetchFromTelegram()" [disabled]="loadingTelegram">
        <span *ngIf="loadingTelegram" class="spinner-border spinner-border-sm me-2" role="status"></span>
        <svg *ngIf="!loadingTelegram" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-1"><path d="M21.198 2.433a2.242 2.242 0 0 0-1.022.215l-16.5 7.07a2.25 2.25 0 0 0-.105 4.05l3.38 1.354 1.353 3.38a2.25 2.25 0 0 0 4.05.105l7.07-16.5a2.242 2.242 0 0 0-2.146-2.824z"></path><path d="m10.5 13.5 3-3"></path></svg>
        Загрузить тендеры из Telegram
      </button>
    </div>

    <!-- Telegram Messages Section -->
    <div *ngIf="telegramMessages.length > 0 || showTelegramSection" class="card mb-4">
      <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0 fw-semibold">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-2"><path d="M21.198 2.433a2.242 2.242 0 0 0-1.022.215l-16.5 7.07a2.25 2.25 0 0 0-.105 4.05l3.38 1.354 1.353 3.38a2.25 2.25 0 0 0 4.05.105l7.07-16.5a2.242 2.242 0 0 0-2.146-2.824z"></path><path d="m10.5 13.5 3-3"></path></svg>
          Сообщения из Telegram
          <span class="badge bg-primary ms-2">{{ telegramMessages.length }}</span>
        </h5>
        <button class="btn btn-sm btn-outline-secondary" (click)="showTelegramSection = false">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover mb-0 align-middle">
            <thead class="table-light">
              <tr>
                <th style="width: 80px;">ID</th>
                <th>Текст</th>
                <th>Ссылка</th>
                <th style="width: 100px;">Статус</th>
                <th style="width: 120px;">Время</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="telegramMessages.length === 0">
                <td colspan="5" class="text-center text-muted py-4">Нет сообщений за сегодня</td>
              </tr>
              <tr *ngFor="let msg of telegramMessages">
                <td class="text-muted">#{{ msg.messageId }}</td>
                <td class="text-truncate" style="max-width: 300px;" [title]="msg.text">{{ msg.text }}</td>
                <td>
                  <a *ngIf="msg.extractedUrl" [href]="msg.extractedUrl" target="_blank" class="text-primary small text-truncate d-inline-block" style="max-width: 200px;">
                    {{ msg.extractedUrl }}
                  </a>
                  <span *ngIf="!msg.extractedUrl" class="text-muted small">—</span>
                </td>
                <td>
                  <span class="badge" [ngClass]="STATUS_CLASSES[msg.status] || 'bg-secondary'">{{ STATUS_LABELS[msg.status] || msg.status }}</span>
                </td>
                <td class="small text-muted">{{ msg.createdAt | date:'HH:mm' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="row g-4 mb-4">
      <div class="col-md-6 col-lg-3" *ngFor="let stat of statCards">
        <div class="card card-hover h-100">
          <div class="card-body d-flex align-items-center justify-content-between">
            <div>
              <p class="text-muted mb-1 small">{{ stat.label }}</p>
              <h4 class="fw-bold mb-0">{{ stat.value }}</h4>
            </div>
            <div class="rounded-3 d-flex align-items-center justify-content-center" [ngStyle]="{'background-color': stat.color, 'width': '48px', 'height': '48px'}">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" [innerHTML]="stat.icon"></svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tenders List -->
    <div class="card">
      <div class="card-header bg-white d-flex align-items-center justify-content-between">
        <h5 class="mb-0 fw-semibold">Тендеры</h5>
        <button class="btn btn-sm btn-outline-primary" (click)="loadTenders()">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
          Обновить
        </button>
      </div>
      <div class="card-body">
        <div class="d-flex gap-3 mb-4">
          <div class="position-relative flex-grow-1" style="max-width: 400px;">
            <svg class="position-absolute top-50 translate-middle-y ms-3" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input type="text" class="form-control ps-5" placeholder="Поиск по номеру, названию..." [(ngModel)]="search" (input)="filterTenders()">
          </div>
          <select class="form-select" style="width: 180px;" [(ngModel)]="statusFilter" (change)="filterTenders()">
            <option value="ALL">Все статусы</option>
            <option *ngFor="let key of statusKeys" [value]="key">{{ STATUS_LABELS[key] }}</option>
          </select>
        </div>

        <div class="d-flex flex-column gap-3">
          <div *ngIf="filtered.length === 0" class="text-center text-muted py-5">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            <p class="mt-2">Тендеры не найдены</p>
          </div>

          <div *ngFor="let tender of filtered" class="card card-hover cursor-pointer" [routerLink]="['/tenders', tender.id]">
            <div class="card-body">
              <div class="d-flex align-items-start justify-content-between">
                <div class="flex-fill">
                  <div class="d-flex align-items-center gap-2 mb-2">
                    <span class="badge" [ngClass]="STATUS_CLASSES[tender.status] || 'bg-secondary'">{{ STATUS_LABELS[tender.status] || tender.status }}</span>
                    <span class="text-muted small">{{ tender.tenderNumber }}</span>
                  </div>
                  <h5 class="fw-semibold mb-2 text-truncate-2">{{ tender.title }}</h5>
                  <div class="d-flex gap-4 text-muted small">
                    <span>{{ tender.organizer }}</span>
                    <span *ngIf="tender.totalAmount" class="fw-medium">{{ tender.totalAmount | plainNumber:0 }} {{ tender.currency }}</span>
                    <span>{{ tender.createdAt | date:'dd.MM.yyyy HH:mm' }}</span>
                  </div>
                </div>
                <div class="d-flex align-items-center gap-2 ms-3">
                  <button class="btn btn-sm btn-link" (click)="openUrl($event, tender.url)">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
                  </button>
                  <button class="btn btn-sm btn-link" (click)="reprocess($event, tender.id)">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
                  </button>
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats: DashboardStats | null = null;
  logs: LogEntry[] = [];
  STATUS_LABELS = STATUS_LABELS;
  STATUS_CLASSES = STATUS_CLASSES;
  private sub!: Subscription;

  // Telegram section
  telegramMessages: TelegramMessage[] = [];
  loadingTelegram = false;
  showTelegramSection = false;

  statCards: { label: string; value: number; color: string; icon: string }[] = [];

  // Tenders section
  tenders: Tender[] = [];
  filtered: Tender[] = [];
  search = '';
  statusFilter = 'ALL';
  statusKeys = Object.keys(STATUS_LABELS).filter(k => !['PENDING','SEARCHING','FOUND_ON_SUPPLIER','MODEL_MATCHED','NOT_FOUND'].includes(k));

  constructor(private tenderService: TenderService) {}

  ngOnInit() {
    this.loadData();
    this.loadTenders();
    this.sub = interval(10000).subscribe(() => {
      this.loadData();
      this.loadTenders();
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  loadData() {
    this.tenderService.getStats().subscribe(stats => {
      this.stats = stats;
      this.updateStatCards();
    });
  }

  loadTenders() {
    this.tenderService.getTenders().subscribe(data => {
      this.tenders = data;
      this.filterTenders();
    });
  }

  filterTenders() {
    let result = [...this.tenders];
    if (this.search) {
      const s = this.search.toLowerCase();
      result = result.filter(t =>
        t.title?.toLowerCase().includes(s) ||
        t.tenderNumber?.toLowerCase().includes(s) ||
        t.organizer?.toLowerCase().includes(s)
      );
    }
    if (this.statusFilter !== 'ALL') {
      result = result.filter(t => t.status === this.statusFilter);
    }
    this.filtered = result;
  }

  fetchFromTelegram() {
    this.loadingTelegram = true;
    this.showTelegramSection = true;
    this.tenderService.fetchTodayMessages().subscribe({
      next: (messages) => {
        this.telegramMessages = messages;
        this.loadingTelegram = false;
      },
      error: (err) => {
        this.loadingTelegram = false;
        this.telegramMessages = [];
        alert('Ошибка загрузки из Telegram: ' + (err.error?.message || err.message || 'Неизвестная ошибка'));
      }
    });
  }

  updateStatCards() {
    if (!this.stats) return;
    this.statCards = [
      { label: 'Всего тендеров', value: this.stats.totalTenders, color: '#3b82f6', icon: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline>' },
      { label: 'Обработано', value: this.stats.completedTenders, color: '#22c55e', icon: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>' },
      { label: 'В обработке', value: this.stats.processingTenders, color: '#eab308', icon: '<line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line><line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line><line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line><line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>' },
      { label: 'Ошибки', value: this.stats.errorTenders, color: '#ef4444', icon: '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>' },
      { label: 'Email отправлено', value: this.stats.totalEmailsSent, color: '#a855f7', icon: '<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline>' },
      { label: 'В очереди', value: this.stats.pendingEmails, color: '#f97316', icon: '<circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline>' },
      { label: 'Товаров найдено', value: this.stats.itemsFound, color: '#14b8a6', icon: '<circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>' },
      { label: 'Всего позиций', value: this.stats.totalItems, color: '#6366f1', icon: '<polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline>' },
    ];
  }

  openUrl(event: Event, url: string) {
    event.stopPropagation();
    window.open(url, '_blank');
  }

  reprocess(event: Event, id: number) {
    event.stopPropagation();
    this.tenderService.reprocessTender(id).subscribe(() => this.loadTenders());
  }
}
