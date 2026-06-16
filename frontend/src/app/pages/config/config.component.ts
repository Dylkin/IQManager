import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TenderService } from '../../services/tender.service';
import { Config, CONFIG_GROUP_LABELS, CONFIG_GROUP_COLORS } from '../../models/models';

@Component({
  selector: 'app-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="mb-4 d-flex align-items-center justify-content-between">
      <h2 class="fw-bold">Переменные окружения</h2>
      <button class="btn btn-primary" (click)="openModal()">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
        Добавить переменную
      </button>
    </div>

    <!-- Group Filters -->
    <div class="mb-4 d-flex gap-2 flex-wrap">
      <button class="btn" [class.btn-primary]="groupFilter === 'ALL'" [class.btn-outline-primary]="groupFilter !== 'ALL'" (click)="setGroupFilter('ALL')">
        Все группы
      </button>
      <button *ngFor="let g of groups" class="btn" [class.btn-primary]="groupFilter === g" [class.btn-outline-primary]="groupFilter !== g" (click)="setGroupFilter(g)">
        {{ groupLabel(g) }}
      </button>
    </div>

    <!-- Search -->
    <div class="position-relative mb-4" style="max-width: 400px;">
      <svg class="position-absolute top-50 translate-middle-y ms-3" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
      <input type="text" class="form-control ps-5" placeholder="Поиск по ключу или описанию..." [(ngModel)]="search" (input)="filter()">
    </div>

    <!-- Config Table -->
    <div class="card">
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover mb-0 align-middle">
            <thead class="table-light">
              <tr>
                <th style="width: 80px;">ID</th>
                <th style="width: 120px;">Группа</th>
                <th>Ключ</th>
                <th>Значение</th>
                <th>Описание</th>
                <th style="width: 100px;">Статус</th>
                <th style="width: 120px;">Обновлено</th>
                <th style="width: 100px;"></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="filtered.length === 0">
                <td colspan="8" class="text-center text-muted py-5">Нет данных</td>
              </tr>
              <tr *ngFor="let c of filtered">
                <td class="text-muted">#{{ c.id }}</td>
                <td><span class="badge" [ngClass]="groupColor(c.group)">{{ groupLabel(c.group) }}</span></td>
                <td><code class="text-primary">{{ c.key }}</code></td>
                <td>
                  <ng-container *ngIf="c.isSecret">
                    <span class="text-muted">••••••••</span>
                    <button class="btn btn-sm btn-link p-0 ms-2" (click)="toggleReveal(c.id)">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                    </button>
                    <span *ngIf="revealed[c.id]" class="ms-2">{{ c.value }}</span>
                  </ng-container>
                  <span *ngIf="!c.isSecret">{{ c.value }}</span>
                </td>
                <td class="text-muted small">{{ c.description }}</td>
                <td>
                  <span *ngIf="!c.isEditable" class="badge bg-danger">Только чтение</span>
                  <span *ngIf="c.isEditable" class="badge bg-success">Редактируемо</span>
                </td>
                <td class="small text-muted">{{ c.updatedAt | date:'dd.MM.yy HH:mm' }}</td>
                <td>
                  <button class="btn btn-sm btn-light me-1" (click)="editConfig(c)" [disabled]="!c.isEditable">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                  </button>
                  <button class="btn btn-sm btn-light text-danger" (click)="deleteConfig(c.id)" [disabled]="!c.isEditable">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div *ngIf="showModal" class="modal d-block" tabindex="-1" style="background-color: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ editingId ? 'Редактировать' : 'Добавить' }} переменную</h5>
            <button type="button" class="btn-close" (click)="closeModal()"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3" *ngIf="!editingId">
              <label class="form-label">Ключ <span class="text-danger">*</span></label>
              <input type="text" class="form-control" [(ngModel)]="form.key" placeholder="telegram.bot.token">
              <div class="form-text">Используйте формат camelCase с точками</div>
            </div>
            <div class="mb-3">
              <label class="form-label">Значение</label>
              <input *ngIf="!form.isSecret" type="text" class="form-control" [(ngModel)]="form.value" placeholder="Значение переменной">
              <input *ngIf="form.isSecret" type="password" class="form-control" [(ngModel)]="form.value" placeholder="••••••••">
            </div>
            <div class="mb-3">
              <label class="form-label">Описание</label>
              <input type="text" class="form-control" [(ngModel)]="form.description" placeholder="Краткое описание">
            </div>
            <div class="mb-3" *ngIf="!editingId">
              <label class="form-label">Группа</label>
              <select class="form-select" [(ngModel)]="form.group">
                <option *ngFor="let g of groups" [value]="g">{{ groupLabel(g) }}</option>
              </select>
            </div>
            <div class="form-check mb-0" *ngIf="!editingId">
              <input class="form-check-input" type="checkbox" [(ngModel)]="form.isSecret" id="isSecret">
              <label class="form-check-label" for="isSecret">Секретное значение (скрыто в интерфейсе)</label>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="closeModal()">Отмена</button>
            <button type="button" class="btn btn-primary" (click)="saveConfig()">{{ editingId ? 'Сохранить' : 'Добавить' }}</button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ConfigComponent implements OnInit {
  configs: Config[] = [];
  filtered: Config[] = [];
  groups: string[] = [];
  groupFilter = 'ALL';
  search = '';
  showModal = false;
  editingId: number | null = null;
  revealed: Record<number, boolean> = {};

  form: Partial<Config> = { key: '', value: '', description: '', group: 'GENERAL', isSecret: false };

  constructor(private tenderService: TenderService) {}

  ngOnInit() {
    this.load();
    this.tenderService.getConfigGroups().subscribe(g => this.groups = g);
  }

  load() {
    this.tenderService.getConfigs().subscribe(data => {
      this.configs = data;
      this.filter();
    });
  }

  filter() {
    let result = [...this.configs];
    if (this.groupFilter !== 'ALL') {
      result = result.filter(c => c.group === this.groupFilter);
    }
    if (this.search) {
      const s = this.search.toLowerCase();
      result = result.filter(c =>
        c.key.toLowerCase().includes(s) ||
        c.description?.toLowerCase().includes(s) ||
        c.value?.toLowerCase().includes(s)
      );
    }
    this.filtered = result;
  }

  setGroupFilter(group: string) {
    this.groupFilter = group;
    this.filter();
  }

  groupLabel(group: string): string {
    return CONFIG_GROUP_LABELS[group] || group;
  }

  groupColor(group: string): string {
    return CONFIG_GROUP_COLORS[group] || 'bg-light text-dark border';
  }

  toggleReveal(id: number) {
    this.revealed[id] = !this.revealed[id];
  }

  openModal() {
    this.editingId = null;
    this.form = { key: '', value: '', description: '', group: 'GENERAL', isSecret: false };
    this.showModal = true;
  }

  editConfig(c: Config) {
    this.editingId = c.id;
    this.form = { value: c.value, description: c.description };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingId = null;
  }

  saveConfig() {
    if (this.editingId) {
      this.tenderService.updateConfig(this.editingId, this.form).subscribe(() => {
        this.closeModal();
        this.load();
      });
    } else {
      this.tenderService.createConfig(this.form).subscribe(() => {
        this.closeModal();
        this.load();
      });
    }
  }

  deleteConfig(id: number) {
    if (confirm('Удалить переменную?')) {
      this.tenderService.deleteConfig(id).subscribe(() => this.load());
    }
  }
}
