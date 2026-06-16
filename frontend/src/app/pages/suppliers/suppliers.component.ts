import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TenderService } from '../../services/tender.service';
import { Supplier } from '../../models/models';

@Component({
  selector: 'app-suppliers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="mb-4 d-flex align-items-center justify-content-between">
      <h2 class="fw-bold">Поставщики</h2>
      <button class="btn btn-primary" (click)="showModal = true; editingSupplier = null; resetForm()">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
        Добавить поставщика
      </button>
    </div>

    <div class="row g-4">
      <div *ngIf="suppliers.length === 0" class="col-12 text-center text-muted py-5">
        <p>Нет добавленных поставщиков</p>
      </div>
      <div *ngFor="let s of suppliers" class="col-md-6">
        <div class="card h-100">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start">
              <div class="flex-fill">
                <div class="d-flex align-items-center gap-2 mb-2">
                  <h5 class="fw-semibold mb-0">{{ s.name }}</h5>
                  <span class="badge" [ngClass]="s.isActive ? 'bg-success' : 'bg-secondary'">{{ s.isActive ? 'Активен' : 'Неактивен' }}</span>
                </div>
                <div class="small text-muted">
                  <p class="mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
                    <a [href]="s.siteUrl" target="_blank" class="ms-1">{{ s.siteUrl }}</a>
                  </p>
                  <p class="mb-1">Email: {{ s.email }}</p>
                  <p class="mb-1">Телефон: {{ s.phone }}</p>
                  <p class="mb-0">Контакт: {{ s.contactPerson }}</p>
                </div>
              </div>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-light" (click)="editSupplier(s)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                </button>
                <button class="btn btn-sm btn-light text-danger" (click)="deleteSupplier(s.id)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div *ngIf="showModal" class="modal d-block" tabindex="-1" style="background-color: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ editingSupplier ? 'Редактировать поставщика' : 'Добавить поставщика' }}</h5>
            <button type="button" class="btn-close" (click)="showModal = false"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">Название *</label>
              <input type="text" class="form-control" [(ngModel)]="formData.name" placeholder="Название компании" required>
            </div>
            <div class="mb-3">
              <label class="form-label">Сайт *</label>
              <input type="text" class="form-control" [(ngModel)]="formData.siteUrl" placeholder="https://example.com" required>
            </div>
            <div class="mb-3">
              <label class="form-label">Email *</label>
              <input type="email" class="form-control" [(ngModel)]="formData.email" placeholder="sales@example.com" required>
            </div>
            <div class="mb-3">
              <label class="form-label">Телефон *</label>
              <input type="text" class="form-control" [(ngModel)]="formData.phone" placeholder="+7 (XXX) XXX-XX-XX" required>
            </div>
            <div class="mb-3">
              <label class="form-label">Контактное лицо *</label>
              <input type="text" class="form-control" [(ngModel)]="formData.contactPerson" placeholder="Имя контактного лица" required>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="showModal = false">Отмена</button>
            <button type="button" class="btn btn-primary" (click)="saveSupplier()" [disabled]="!formData.name?.trim() || !formData.siteUrl?.trim() || !formData.email?.trim() || !formData.phone?.trim() || !formData.contactPerson?.trim()">{{ editingSupplier ? 'Сохранить' : 'Добавить' }}</button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class SuppliersComponent implements OnInit {
  suppliers: Supplier[] = [];
  showModal = false;
  editingSupplier: Supplier | null = null;
  formData: Partial<Supplier> = {};

  constructor(private tenderService: TenderService) {}

  ngOnInit() {
    this.loadSuppliers();
  }

  loadSuppliers() {
    this.tenderService.getSuppliers().subscribe(data => this.suppliers = data);
  }

  resetForm() {
    this.formData = { name: '', siteUrl: '', email: '', phone: '', contactPerson: '' };
  }

  editSupplier(s: Supplier) {
    this.editingSupplier = s;
    this.formData = { ...s };
    this.showModal = true;
  }

  saveSupplier() {
    const data = { ...this.formData, isActive: true };
    if (this.editingSupplier) {
      this.tenderService.updateSupplier(this.editingSupplier.id, data).subscribe(() => {
        this.showModal = false;
        this.loadSuppliers();
      });
    } else {
      this.tenderService.createSupplier(data).subscribe(() => {
        this.showModal = false;
        this.loadSuppliers();
      });
    }
  }

  deleteSupplier(id: number) {
    if (confirm('Удалить поставщика?')) {
      this.tenderService.deleteSupplier(id).subscribe(() => this.loadSuppliers());
    }
  }
}
