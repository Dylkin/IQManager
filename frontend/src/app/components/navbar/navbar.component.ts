import { Component, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  template: `
    <nav class="bg-white border-end position-fixed top-0 start-0 h-100 d-flex flex-column" style="width: 260px; z-index: 1000;">
      <div class="p-4 border-bottom">
        <div class="d-flex align-items-center gap-3">
          <div class="bg-primary rounded-3 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
          </div>
          <div>
            <h5 class="fw-bold mb-0">IQManager</h5>
            <p class="text-muted mb-0" style="font-size: 0.75rem;">Автоматизация тендеров</p>
          </div>
        </div>
      </div>

      <div class="flex-fill p-3 d-flex flex-column gap-1 overflow-auto">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="sidebar-link">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          Тендеры
        </a>
        <a routerLink="/suppliers" routerLinkActive="active" class="sidebar-link">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          Поставщики
        </a>
        <a routerLink="/equipment-catalog" routerLinkActive="active" class="sidebar-link">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
          Каталог оборудования
        </a>
        <a routerLink="/logs" routerLinkActive="active" class="sidebar-link">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
          Журнал
        </a>
        <a routerLink="/config" routerLinkActive="active" class="sidebar-link">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path></svg>
          Переменные окружения
        </a>
        <a *ngIf="isAdmin" routerLink="/users" routerLinkActive="active" class="sidebar-link">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          Пользователи
        </a>
      </div>

      <div class="p-3 border-top">
        <div *ngIf="user" class="d-flex align-items-center gap-2 mb-2">
          <div class="rounded-circle bg-secondary text-white d-flex align-items-center justify-content-center flex-shrink-0" style="width: 36px; height: 36px; font-size: 0.85rem;">
            {{ initials }}
          </div>
          <div class="text-truncate">
            <p class="mb-0 small fw-medium text-truncate">{{ user.fullName || user.email }}</p>
            <p class="mb-0 text-muted" style="font-size: 0.7rem;">{{ user.role === 'ADMIN' ? 'Администратор' : 'Пользователь' }}</p>
          </div>
        </div>
        <button *ngIf="user" class="btn btn-outline-danger btn-sm w-100" (click)="logout()">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-1"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          Выйти
        </button>
      </div>

      <div class="p-4 border-top text-center">
        <p class="text-muted mb-0" style="font-size: 0.75rem;">IQManager v1.0</p>
      </div>
    </nav>
  `,
})
export class NavbarComponent implements OnInit {
  user: User | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.authService.getCurrentUser().subscribe(user => {
      this.user = user;
    });
  }

  get isAdmin(): boolean {
    return this.user?.role === 'ADMIN';
  }

  get initials(): string {
    if (!this.user) return '';
    if (this.user.fullName) {
      return this.user.fullName.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
    }
    return this.user.email.substring(0, 2).toUpperCase();
  }

  logout() {
    this.authService.logout();
  }
}
