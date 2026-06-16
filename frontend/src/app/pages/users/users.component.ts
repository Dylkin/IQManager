import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TenderService } from '../../services/tender.service';
import { User } from '../../models/models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="d-flex align-items-center justify-content-between mb-4">
      <h4 class="fw-bold mb-0">Справочник пользователей</h4>
      <button class="btn btn-primary" (click)="openCreate()">+ Добавить пользователя</button>
    </div>

    <div class="card">
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>ID</th>
                <th>Email</th>
                <th>Имя</th>
                <th>Роль</th>
                <th>Статус</th>
                <th style="width: 160px">Действия</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of users">
                <td>{{ user.id }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.fullName || '—' }}</td>
                <td>
                  <span class="badge" [ngClass]="user.role === 'ADMIN' ? 'bg-danger' : 'bg-secondary'">{{ user.role === 'ADMIN' ? 'Администратор' : 'Пользователь' }}</span>
                </td>
                <td>
                  <span class="badge" [ngClass]="user.status === 'ACTIVE' ? 'bg-success' : 'bg-warning text-dark'">{{ user.status === 'ACTIVE' ? 'Активен' : 'Заблокирован' }}</span>
                </td>
                <td>
                  <button class="btn btn-sm btn-outline-primary me-1" (click)="openEdit(user)">Изменить</button>
                  <button class="btn btn-sm btn-outline-danger" (click)="deleteUser(user)" [disabled]="user.email === 'pavel.dylkin@gmail.com'">Удалить</button>
                </td>
              </tr>
              <tr *ngIf="users.length === 0">
                <td colspan="6" class="text-center text-muted py-4">Пользователи не найдены</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div *ngIf="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background: rgba(0,0,0,0.4); z-index: 2000;">
      <div class="card shadow" style="width: 100%; max-width: 480px;">
        <div class="card-header d-flex justify-content-between align-items-center">
          <span class="fw-medium">{{ editingUser ? 'Редактирование пользователя' : 'Новый пользователь' }}</span>
          <button class="btn-close" (click)="closeModal()"></button>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label small fw-medium">Email</label>
            <input type="email" class="form-control" [(ngModel)]="form.email" name="formEmail" required>
          </div>
          <div class="mb-3">
            <label class="form-label small fw-medium">Полное имя</label>
            <input type="text" class="form-control" [(ngModel)]="form.fullName" name="formFullName">
          </div>
          <div class="mb-3" *ngIf="!editingUser">
            <label class="form-label small fw-medium">Пароль</label>
            <input type="password" class="form-control" [(ngModel)]="form.password" name="formPassword" required minlength="6">
          </div>
          <div class="row g-2">
            <div class="col-6">
              <label class="form-label small fw-medium">Роль</label>
              <select class="form-select" [(ngModel)]="form.role" name="formRole">
                <option value="USER">Пользователь</option>
                <option value="ADMIN">Администратор</option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small fw-medium">Статус</label>
              <select class="form-select" [(ngModel)]="form.status" name="formStatus">
                <option value="ACTIVE">Активен</option>
                <option value="BLOCKED">Заблокирован</option>
              </select>
            </div>
          </div>
        </div>
        <div class="card-footer d-flex justify-content-end gap-2">
          <button class="btn btn-outline-secondary" (click)="closeModal()">Отмена</button>
          <button class="btn btn-primary" (click)="saveUser()">Сохранить</button>
        </div>
      </div>
    </div>
  `
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  showModal = false;
  editingUser: User | null = null;
  form = { email: '', fullName: '', password: '', role: 'USER' as 'USER' | 'ADMIN', status: 'ACTIVE' as 'ACTIVE' | 'BLOCKED' };
  error = '';

  constructor(private tenderService: TenderService) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.tenderService.getUsers().subscribe({
      next: users => this.users = users,
      error: () => this.error = 'Ошибка загрузки пользователей'
    });
  }

  openCreate() {
    this.editingUser = null;
    this.form = { email: '', fullName: '', password: '', role: 'USER', status: 'ACTIVE' };
    this.showModal = true;
  }

  openEdit(user: User) {
    this.editingUser = user;
    this.form = { email: user.email, fullName: user.fullName || '', password: '', role: user.role, status: user.status };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingUser = null;
  }

  saveUser() {
    if (this.editingUser) {
      this.tenderService.updateUser(this.editingUser.id, {
        email: this.form.email,
        fullName: this.form.fullName,
        role: this.form.role,
        status: this.form.status
      }).subscribe({
        next: () => { this.closeModal(); this.loadUsers(); },
        error: (err) => { alert(err.error?.message || 'Ошибка сохранения'); }
      });
    } else {
      this.tenderService.createUser({
        email: this.form.email,
        fullName: this.form.fullName,
        password: this.form.password,
        role: this.form.role,
        status: this.form.status
      }).subscribe({
        next: () => { this.closeModal(); this.loadUsers(); },
        error: (err) => { alert(err.error?.message || 'Ошибка создания'); }
      });
    }
  }

  deleteUser(user: User) {
    if (!confirm(`Удалить пользователя ${user.email}?`)) return;
    this.tenderService.deleteUser(user.id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => { alert(err.error?.message || 'Ошибка удаления'); }
    });
  }
}
