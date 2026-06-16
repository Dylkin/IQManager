import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { EquipmentCatalogService } from '../../services/equipment-catalog.service';
import { EquipmentCatalogItem, EquipmentCharacteristic, EquipmentType, Manufacturer } from '../../models/models';

@Component({
  selector: 'app-equipment-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container-fluid py-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold mb-0">Каталог оборудования</h2>
        <div class="d-flex align-items-center gap-3">
          <span class="text-muted">Всего моделей: {{ items.length }}</span>
          <button class="btn btn-primary" (click)="openModal()">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-1"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            Добавить модель
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="card mb-4">
        <div class="card-body d-flex gap-3 flex-wrap">
          <div>
            <label class="form-label small text-muted mb-1">Тип оборудования</label>
            <select class="form-select form-select-sm" [(ngModel)]="filterTypeId" (change)="applyFilters()">
              <option [ngValue]="null">Все типы</option>
              <option *ngFor="let t of types" [ngValue]="t.id">{{ t.name }}</option>
            </select>
          </div>
          <div>
            <label class="form-label small text-muted mb-1">Производитель</label>
            <select class="form-select form-select-sm" [(ngModel)]="filterManufacturerId" (change)="applyFilters()">
              <option [ngValue]="null">Все производители</option>
              <option *ngFor="let m of manufacturers" [ngValue]="m.id">{{ m.name }}</option>
            </select>
          </div>
          <div class="ms-auto d-flex align-items-end">
            <button class="btn btn-outline-secondary btn-sm" (click)="resetFilters()">Сбросить</button>
          </div>
        </div>
      </div>

      <!-- Items table -->
      <div class="card">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th>Тип оборудования</th>
                  <th>Производитель</th>
                  <th>Модель</th>
                  <th>Артикул</th>
                  <th style="width: 120px;">Характеристик</th>
                  <th style="width: 150px;"></th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let item of filteredItems" (click)="toggleExpand(item)" style="cursor: pointer;">
                  <td>{{ item.equipmentType?.name || '—' }}</td>
                  <td>{{ item.manufacturer?.name || '—' }}</td>
                  <td class="fw-medium">{{ item.modelName }}</td>
                  <td>{{ item.modelNumber || '—' }}</td>
                  <td><span class="badge bg-light text-dark border">{{ item.specs?.length || 0 }}</span></td>
                  <td>
                    <button class="btn btn-link btn-sm p-0 text-primary me-2" (click)="openModal(item); $event.stopPropagation()">
                      Изменить
                    </button>
                    <button class="btn btn-link btn-sm p-0 text-danger me-2" (click)="deleteItem(item); $event.stopPropagation()">
                      Удалить
                    </button>
                    <button class="btn btn-link btn-sm p-0 text-muted" (click)="toggleExpand(item); $event.stopPropagation()">
                      {{ expandedItemId === item.id ? 'Скрыть' : 'Подробнее' }}
                    </button>
                  </td>
                </tr>
                <tr *ngIf="filteredItems.length === 0">
                  <td colspan="6" class="text-center text-muted py-4">Модели не найдены</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Expanded detail -->
      <div *ngIf="expandedItem" class="card mt-3">
        <div class="card-header d-flex justify-content-between align-items-center">
          <span class="fw-medium">{{ expandedItem.equipmentType?.name }} — {{ expandedItem.manufacturer?.name }} {{ expandedItem.modelName }}</span>
          <div>
            <button class="btn btn-link btn-sm p-0 text-primary me-3" (click)="openModal(expandedItem)">Изменить</button>
            <button class="btn btn-link btn-sm p-0 text-muted" (click)="expandedItemId = null; expandedItem = null">Закрыть</button>
          </div>
        </div>
        <div class="card-body">
          <div class="row g-3">
            <div class="col-md-4">
              <p class="small text-muted mb-1">Тип оборудования</p>
              <p class="fw-medium mb-0">{{ expandedItem.equipmentType?.name || '—' }}</p>
            </div>
            <div class="col-md-4">
              <p class="small text-muted mb-1">Производитель</p>
              <p class="fw-medium mb-0">{{ expandedItem.manufacturer?.name || '—' }}</p>
            </div>
            <div class="col-md-4">
              <p class="small text-muted mb-1">Модель</p>
              <p class="fw-medium mb-0">{{ expandedItem.modelName }}</p>
            </div>
            <div class="col-md-4" *ngIf="expandedItem.modelNumber">
              <p class="small text-muted mb-1">Артикул</p>
              <p class="fw-medium mb-0">{{ expandedItem.modelNumber }}</p>
            </div>
          </div>
          <hr *ngIf="expandedItem.specs?.length">
          <h6 *ngIf="expandedItem.specs?.length" class="fw-bold mb-3">Характеристики</h6>
          <div *ngIf="expandedItem.specs?.length" class="row g-2">
            <div class="col-md-4 col-lg-3" *ngFor="let spec of expandedItem.specs">
              <div class="p-2 rounded border">
                <p class="small text-muted mb-1">{{ spec.characteristic?.label || spec.characteristic?.key || '—' }}</p>
                <p class="fw-semibold mb-0">{{ spec.value }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div *ngIf="showModal" class="modal fade show d-block" tabindex="-1" style="background-color: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ editingItem ? 'Редактировать модель' : 'Добавить модель' }}</h5>
            <button type="button" class="btn-close" (click)="closeModal()"></button>
          </div>
          <div class="modal-body">
            <!-- Type -->
            <div class="mb-3">
              <label class="form-label">Тип оборудования</label>
              <div class="d-flex gap-2">
                <select class="form-select" [(ngModel)]="form.equipmentTypeId" [disabled]="showNewType" (change)="onTypeChange()">
                  <option [ngValue]="null">Выберите тип</option>
                  <option *ngFor="let t of types" [ngValue]="t.id">{{ t.name }}</option>
                </select>
                <button class="btn btn-outline-secondary" (click)="showNewType = !showNewType" [class.active]="showNewType">
                  Новый
                </button>
              </div>
              <div *ngIf="showNewType" class="row g-2 mt-2">
                <div class="col-md-6">
                  <input type="text" class="form-control" placeholder="Название типа" [(ngModel)]="newTypeName">
                </div>
                <div class="col-md-6">
                  <input type="text" class="form-control" placeholder="Код (лат.)" [(ngModel)]="newTypeCode">
                </div>
              </div>
            </div>

            <!-- Manufacturer -->
            <div class="mb-3">
              <label class="form-label">Производитель</label>
              <div class="d-flex gap-2">
                <select class="form-select" [(ngModel)]="form.manufacturerId" [disabled]="showNewManufacturer">
                  <option [ngValue]="null">Выберите производителя</option>
                  <option *ngFor="let m of manufacturers" [ngValue]="m.id">{{ m.name }}</option>
                </select>
                <button class="btn btn-outline-secondary" (click)="showNewManufacturer = !showNewManufacturer" [class.active]="showNewManufacturer">
                  Новый
                </button>
              </div>
              <div *ngIf="showNewManufacturer" class="row g-2 mt-2">
                <div class="col-md-4">
                  <input type="text" class="form-control" placeholder="Название" [(ngModel)]="newManufacturerName">
                </div>
                <div class="col-md-4">
                  <input type="text" class="form-control" placeholder="Страна" [(ngModel)]="newManufacturerCountry">
                </div>
                <div class="col-md-4">
                  <input type="text" class="form-control" placeholder="Сайт" [(ngModel)]="newManufacturerWebsite">
                </div>
              </div>
            </div>

            <!-- Model -->
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label">Модель <span class="text-danger">*</span></label>
                <input type="text" class="form-control" [(ngModel)]="form.modelName" placeholder="Название модели">
              </div>
              <div class="col-md-6">
                <label class="form-label">Артикул / модельный номер</label>
                <input type="text" class="form-control" [(ngModel)]="form.modelNumber" placeholder="Артикул">
              </div>
            </div>

            <!-- Specs -->
            <div class="mb-3" *ngIf="selectedTypeCharacteristics.length > 0">
              <label class="form-label">Характеристики</label>
              <div class="row g-2">
                <div class="col-md-6" *ngFor="let ch of selectedTypeCharacteristics">
                  <label class="form-label small text-muted mb-1">{{ ch.label || ch.key }}{{ ch.unit ? ', ' + ch.unit : '' }}</label>
                  <input type="text" class="form-control form-control-sm" [(ngModel)]="form.specValues[ch.id]" [placeholder]="ch.label || ch.key">
                </div>
              </div>
            </div>
            <div *ngIf="form.equipmentTypeId && selectedTypeCharacteristics.length === 0" class="alert alert-light border text-muted small">
              У выбранного типа нет характеристик. Сохраните модель, характеристики можно добавить позже.
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="closeModal()">Отмена</button>
            <button class="btn btn-primary" (click)="save()" [disabled]="loading">
              <span *ngIf="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
              Сохранить
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class EquipmentCatalogComponent implements OnInit {
  items: EquipmentCatalogItem[] = [];
  filteredItems: EquipmentCatalogItem[] = [];
  types: EquipmentType[] = [];
  manufacturers: Manufacturer[] = [];
  filterTypeId: number | null = null;
  filterManufacturerId: number | null = null;
  expandedItemId: number | null = null;
  expandedItem: EquipmentCatalogItem | null = null;

  showModal = false;
  loading = false;
  editingItem: EquipmentCatalogItem | null = null;

  form = {
    equipmentTypeId: null as number | null,
    manufacturerId: null as number | null,
    modelName: '',
    modelNumber: '',
    specValues: {} as Record<number, string>
  };

  showNewType = false;
  newTypeName = '';
  newTypeCode = '';

  showNewManufacturer = false;
  newManufacturerName = '';
  newManufacturerCountry = '';
  newManufacturerWebsite = '';

  constructor(private catalogService: EquipmentCatalogService) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.catalogService.getItems().subscribe(items => {
      this.items = items;
      this.applyFilters();
    });
    this.catalogService.getTypes().subscribe(types => this.types = types);
    this.catalogService.getManufacturers().subscribe(m => this.manufacturers = m);
  }

  applyFilters() {
    this.filteredItems = this.items.filter(item => {
      if (this.filterTypeId && item.equipmentType?.id !== this.filterTypeId) return false;
      if (this.filterManufacturerId && item.manufacturer?.id !== this.filterManufacturerId) return false;
      return true;
    });
  }

  resetFilters() {
    this.filterTypeId = null;
    this.filterManufacturerId = null;
    this.applyFilters();
  }

  toggleExpand(item: EquipmentCatalogItem) {
    if (this.expandedItemId === item.id) {
      this.expandedItemId = null;
      this.expandedItem = null;
    } else {
      this.expandedItemId = item.id;
      this.expandedItem = item;
    }
  }

  get selectedTypeCharacteristics(): EquipmentCharacteristic[] {
    if (!this.form.equipmentTypeId) return [];
    const type = this.types.find(t => t.id === this.form.equipmentTypeId);
    return type?.characteristics || [];
  }

  openModal(item?: EquipmentCatalogItem) {
    this.editingItem = item || null;
    this.showNewType = false;
    this.newTypeName = '';
    this.newTypeCode = '';
    this.showNewManufacturer = false;
    this.newManufacturerName = '';
    this.newManufacturerCountry = '';
    this.newManufacturerWebsite = '';

    if (item) {
      this.form = {
        equipmentTypeId: item.equipmentType?.id || null,
        manufacturerId: item.manufacturer?.id || null,
        modelName: item.modelName,
        modelNumber: item.modelNumber || '',
        specValues: {}
      };
      for (const spec of item.specs || []) {
        if (spec.characteristic?.id != null) {
          this.form.specValues[spec.characteristic.id] = spec.value;
        }
      }
    } else {
      this.form = {
        equipmentTypeId: null,
        manufacturerId: null,
        modelName: '',
        modelNumber: '',
        specValues: {}
      };
    }
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingItem = null;
  }

  onTypeChange() {
    // Keep spec values only when editing the same type; otherwise reset
    if (this.editingItem && this.editingItem.equipmentType?.id === this.form.equipmentTypeId) {
      this.form.specValues = {};
      for (const spec of this.editingItem.specs || []) {
        if (spec.characteristic?.id != null) {
          this.form.specValues[spec.characteristic.id] = spec.value;
        }
      }
    } else {
      this.form.specValues = {};
    }
  }

  async save() {
    if (!this.form.modelName.trim()) {
      alert('Введите название модели');
      return;
    }

    let equipmentTypeId = this.form.equipmentTypeId;
    if (this.showNewType) {
      const name = this.newTypeName.trim();
      const code = this.newTypeCode.trim();
      if (!name) {
        alert('Введите название нового типа');
        return;
      }
      try {
        const type = await firstValueFrom(this.catalogService.createType({ name, code: code || this.generateCode(name) }));
        this.types.push(type);
        equipmentTypeId = type.id;
      } catch (err: any) {
        alert('Ошибка создания типа: ' + (err.error?.message || err.message));
        return;
      }
    }
    if (!equipmentTypeId) {
      alert('Выберите тип оборудования');
      return;
    }

    let manufacturerId = this.form.manufacturerId;
    if (this.showNewManufacturer) {
      const name = this.newManufacturerName.trim();
      if (!name) {
        alert('Введите название производителя');
        return;
      }
      try {
        const m = await firstValueFrom(this.catalogService.createManufacturer({
          name,
          country: this.newManufacturerCountry.trim() || undefined,
          website: this.newManufacturerWebsite.trim() || undefined
        }));
        this.manufacturers.push(m);
        manufacturerId = m.id;
      } catch (err: any) {
        alert('Ошибка создания производителя: ' + (err.error?.message || err.message));
        return;
      }
    }
    if (!manufacturerId) {
      alert('Выберите производителя');
      return;
    }

    const specValues: Record<number, string> = {};
    for (const ch of this.selectedTypeCharacteristics) {
      const value = this.form.specValues[ch.id]?.trim();
      if (value) {
        specValues[ch.id] = value;
      }
    }

    const payload = {
      equipmentTypeId,
      manufacturerId,
      modelName: this.form.modelName.trim(),
      modelNumber: this.form.modelNumber.trim() || undefined,
      specValues
    };

    this.loading = true;
    const request = this.editingItem
      ? this.catalogService.updateItem(this.editingItem.id, payload)
      : this.catalogService.createItem(payload);

    request.subscribe({
      next: () => {
        this.loading = false;
        this.closeModal();
        this.loadData();
      },
      error: (err: any) => {
        this.loading = false;
        alert('Ошибка сохранения: ' + (err.error?.message || err.message || 'Неизвестная ошибка'));
      }
    });
  }

  deleteItem(item: EquipmentCatalogItem) {
    if (!confirm(`Удалить модель «${item.modelName}»?`)) return;
    this.catalogService.deleteItem(item.id).subscribe({
      next: () => {
        if (this.expandedItemId === item.id) {
          this.expandedItemId = null;
          this.expandedItem = null;
        }
        this.loadData();
      },
      error: (err: any) => {
        alert('Ошибка удаления: ' + (err.error?.message || err.message || 'Неизвестная ошибка'));
      }
    });
  }

  private generateCode(name: string): string {
    const code = name.toLowerCase().replace(/[^a-z0-9а-яё]/g, '_').replace(/_+/g, '_').replace(/^_+|_+$/g, '');
    return code || 'unknown';
  }
}
