import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
      <div class="card shadow-sm" style="width: 100%; max-width: 420px;">
        <div class="card-body p-4">
          <div class="text-center mb-4">
            <div class="d-inline-flex align-items-center justify-content-center rounded-circle bg-primary text-white mb-3" style="width: 56px; height: 56px;">
              <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
            </div>
            <h4 class="fw-bold mb-1">IQManager</h4>
            <p class="text-muted small mb-0">Вход в систему</p>
          </div>

          <div *ngIf="error" class="alert alert-danger py-2 small">{{ error }}</div>

          <form (ngSubmit)="onSubmit()">
            <div class="mb-3">
              <label class="form-label small fw-medium">Email</label>
              <input type="email" class="form-control" [(ngModel)]="email" name="email" required placeholder="name@company.com">
            </div>
            <div class="mb-4">
              <label class="form-label small fw-medium">Пароль</label>
              <input type="password" class="form-control" [(ngModel)]="password" name="password" required placeholder="••••••••">
            </div>
            <button type="submit" class="btn btn-primary w-100" [disabled]="loading">
              <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
              {{ loading ? 'Вход...' : 'Войти' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    this.error = '';
    this.loading = true;
    this.authService.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error = err.message || 'Ошибка входа';
        this.loading = false;
      }
    });
  }
}
