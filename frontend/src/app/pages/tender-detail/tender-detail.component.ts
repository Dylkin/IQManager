import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TenderService } from '../../services/tender.service';
import { PlainNumberPipe } from '../../pipes/plain-number.pipe';
import { Tender, TenderItem, LogEntry, FoundModel, Supplier, STATUS_CLASSES, STATUS_LABELS } from '../../models/models';


@Component({
  selector: 'app-tender-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, PlainNumberPipe],
  template: `
    <div *ngIf="tender">
      <div class="mb-4 d-flex align-items-center justify-content-between">
        <div class="d-flex align-items-center gap-3">
          <a routerLink="/" class="btn btn-light">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
            Назад
          </a>
          <div>
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="badge" [ngClass]="STATUS_CLASSES[tender.status] || 'bg-secondary'">{{ STATUS_LABELS[tender.status] || tender.status }}</span>
              <span class="text-muted small">{{ tender.tenderNumber }}</span>
            </div>
            <h2 class="fw-bold mb-0">{{ tender.title }}</h2>
          </div>
        </div>
        <div class="d-flex gap-2">
          <a [href]="tender.url" target="_blank" class="btn btn-outline-primary">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
            Открыть на goszakupki.by
          </a>
          <button class="btn btn-outline-secondary" (click)="reprocess()">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
            Обработать заново
          </button>
          <button class="btn btn-outline-secondary" (click)="loadData()">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
            Обновить
          </button>
        </div>
      </div>

      <div class="row g-3 mb-4">
        <div class="col-md-3" *ngFor="let info of infoCards">
          <div class="card">
            <div class="card-body">
              <div class="d-flex align-items-center gap-2 text-muted mb-1 small">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" [innerHTML]="info.icon"></svg>
                {{ info.label }}
              </div>
              <p class="fw-semibold mb-0">{{ info.value }}</p>
            </div>
          </div>
        </div>
      </div>

      <ul class="nav nav-tabs mb-4">
        <li class="nav-item">
          <button class="nav-link" [class.active]="activeTab === 'items'" (click)="activeTab = 'items'">
            Лоты ({{ items.length }})
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link" [class.active]="activeTab === 'logs'" (click)="activeTab = 'logs'">
            Логи ({{ logs.length }})
          </button>
        </li>
      </ul>

      <div *ngIf="activeTab === 'items'">
        <div *ngIf="items.length === 0" class="text-center text-muted py-5">Лоты не найдены</div>
        <div class="table-responsive" *ngIf="items.length > 0">
          <table class="table table-bordered table-hover align-middle" style="table-layout: fixed; min-width: 600px;">
            <thead class="table-light">
              <tr>
                <th style="width: 90px">№ лота</th>
                <th style="width: 50%">Предмет закупки</th>
                <th style="width: 120px">Количество</th>
                <th style="width: 160px">Предельная стоимость</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of items">
                <td class="text-center">
                  <span class="fw-semibold">{{ item.lotNumber }}</span>
                  <div class="mt-1">
                    <span class="badge" [ngClass]="STATUS_CLASSES[item.status] || 'bg-secondary'">{{ STATUS_LABELS[item.status] || item.status }}</span>
                  </div>
                </td>
                <td style="word-wrap: break-word; white-space: normal;">
                  <p class="fw-medium mb-1">{{ item.description }}</p>
                  <div *ngIf="item.originalDescription && item.originalDescription !== item.description" class="p-2 rounded mt-1" style="background-color: #fffbeb;">
                    <p class="small fw-medium text-warning mb-1">Исходное описание с сайта закупок:</p>
                    <p class="small mb-0 text-muted">{{ item.originalDescription }}</p>
                  </div>
                  <div *ngIf="item.okpd2Code" class="mt-1">
                    <span class="badge bg-light text-dark border">{{ item.okpd2Code }}</span>
                  </div>
                </td>
                <td>
                  <span class="fw-semibold">{{ item.quantity }}</span> {{ item.unit }}
                </td>
                <td>
                  <div *ngIf="item.estimatedPrice; else noPrice">
                    <span class="fw-semibold">{{ item.estimatedPrice | plainNumber:0 }}</span> {{ item.currency }}
                  </div>
                  <ng-template #noPrice><span class="text-muted">—</span></ng-template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Описание лота -->
        <h5 class="fw-bold mt-4 mb-3">Описание лота</h5>
        <div class="row g-3">
          <div class="col-12" *ngFor="let item of items">
            <div class="card">
              <div class="card-header d-flex justify-content-between align-items-center">
                <span class="fw-medium">Лот {{ item.lotNumber }} — {{ item.description | slice:0:60 }}{{ item.description.length > 60 ? '...' : '' }}</span>
                <a *ngIf="item.documentFileName" [href]="'/api/documents/' + item.documentFileName" target="_blank" class="small text-primary">Открыть документ ↗</a>
              </div>
              <div class="card-body">
                <textarea class="form-control mb-3" rows="6" [(ngModel)]="item.documentDescription" style="resize: vertical; font-family: monospace;"></textarea>
                <div class="d-flex gap-2 mb-2">
                  <button class="btn btn-primary" (click)="saveEdit(item)">Сохранить</button>
                  <button class="btn btn-outline-secondary" (click)="cancelEdit(item)">Отменить изменения</button>
                  <button class="btn btn-outline-info" [disabled]="extractingParamsItemId === item.id" (click)="extractParameters(item)">
                    <span *ngIf="extractingParamsItemId === item.id" class="spinner-border spinner-border-sm me-1"></span>
                    <svg *ngIf="extractingParamsItemId !== item.id" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-1"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                    Извлечь характеристики
                  </button>
                </div>
                <!-- Прогресс-бар извлечения параметров -->
                <div *ngIf="extractingParamsItemId === item.id" class="progress mb-3" style="height: 4px;">
                  <div class="progress-bar progress-bar-striped progress-bar-animated bg-info" style="width: 100%"></div>
                </div>

                <!-- Извлечённые характеристики -->
                <div *ngIf="item.extractedParams || editingParamsItemId === item.id" class="mb-3 p-2 rounded" style="background-color: #f0f9ff;">
                  <div class="d-flex justify-content-between align-items-center mb-1">
                    <p class="small fw-medium text-info mb-0">Извлечённые характеристики:</p>
                    <button *ngIf="editingParamsItemId !== item.id" class="btn btn-link btn-sm p-0 text-info" (click)="startEditParams(item)">Редактировать</button>
                  </div>

                  <!-- Режим просмотра -->
                  <div *ngIf="editingParamsItemId !== item.id" class="d-flex flex-wrap gap-1">
                    <span *ngFor="let p of parseParamList(item.extractedParams)" class="badge bg-light text-dark border d-inline-flex align-items-center gap-1">
                      {{ p.label }}: {{ p.value }}
                      <button class="btn btn-link btn-sm p-0 text-danger" style="line-height: 1; font-size: 16px; text-decoration: none;" (click)="deleteParam(item, p.key)">×</button>
                    </span>
                  </div>

                  <!-- Режим редактирования -->
                  <div *ngIf="editingParamsItemId === item.id">
                    <div *ngFor="let p of getParamEdits(item.id); let i = index" class="d-flex gap-2 mb-2 align-items-center">
                      <input type="text" class="form-control form-control-sm" [(ngModel)]="p.key" placeholder="Ключ" style="max-width: 160px;">
                      <input type="text" class="form-control form-control-sm" [(ngModel)]="p.label" placeholder="Характеристика">
                      <input type="text" class="form-control form-control-sm" [(ngModel)]="p.value" placeholder="Значение">
                      <button class="btn btn-outline-danger btn-sm" (click)="removeParamEdit(item.id, i)">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                      </button>
                    </div>
                    <div class="d-flex gap-2 mb-2">
                      <button class="btn btn-outline-success btn-sm" (click)="addParamEdit(item.id)">+ Добавить параметр</button>
                    </div>
                    <div class="d-flex gap-2">
                      <button class="btn btn-primary btn-sm" (click)="saveParams(item)">Сохранить</button>
                      <button class="btn btn-outline-secondary btn-sm" (click)="cancelEditParams(item)">Отменить</button>
                    </div>
                  </div>
                </div>

                <div class="d-flex gap-2 mb-2">
                  <button class="btn btn-success" [disabled]="searchingItemId === item.id" (click)="searchByDescription(item)">
                    <span *ngIf="searchingItemId === item.id" class="spinner-border spinner-border-sm me-1"></span>
                    <svg *ngIf="searchingItemId !== item.id" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-1"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    Искать по характеристикам
                  </button>
                </div>
                <!-- Прогресс-бар поиска -->
                <div *ngIf="searchingItemId === item.id" class="progress mb-3" style="height: 4px;">
                  <div class="progress-bar progress-bar-striped progress-bar-animated bg-success" style="width: 100%"></div>
                </div>

                <!-- Результаты поиска -->
                <div *ngIf="item.foundModelName || item.status === 'NOT_FOUND' || item.status === 'FOUND_ON_SUPPLIER' || item.status === 'MODEL_MATCHED'" class="border-top pt-3">
                  <div *ngIf="item.foundModelName" class="row g-3">
                    <div class="col-md-6">
                      <p class="small fw-medium text-success mb-1">Найденная модель:</p>
                      <p class="mb-1 fw-semibold">{{ item.foundModelName }}</p>
                      <table class="table table-bordered table-sm mt-2 mb-2" style="max-width: 700px;">
                        <thead class="table-light">
                          <tr>
                            <th>Цена RUR</th>
                            <th>Курс НБ РБ: 1 RUR</th>
                            <th>Доставка BYN</th>
                            <th>Надбавка %</th>
                            <th>Цена BYN</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>{{ item.foundModelPrice | plainNumber:0 }}</td>
                            <td>{{ item.foundModelExchangeRate | plainNumber:4 }}</td>
                            <td>
                              <input type="number" class="form-control form-control-sm" [(ngModel)]="item.deliveryCostByn" (change)="updatePricing(item)" min="0" step="0.01" placeholder="0">
                            </td>
                            <td>
                              <input type="number" class="form-control form-control-sm" [(ngModel)]="item.markupPercent" (change)="updatePricing(item)" min="0" step="0.1" placeholder="0">
                            </td>
                            <td><strong class="text-success">{{ item.finalPriceByn | plainNumber:2 }}</strong></td>
                          </tr>
                        </tbody>
                      </table>
                      <a *ngIf="item.foundModelUrl" [href]="item.foundModelUrl" target="_blank" class="small text-primary">Открыть на сайте поставщика →</a>
                      <div *ngIf="item.selectedFoundModelId" class="mt-2">
                        <button class="btn btn-primary btn-sm" (click)="saveSelectedModel(item)">
                          Сохранить выбор
                        </button>
                        <span class="small text-muted ms-2">Выбрана другая модель. Нажмите «Сохранить», чтобы запомнить выбор.</span>
                      </div>
                    </div>
                    <div class="col-md-6" *ngIf="item.supplierSite">
                      <p class="small fw-medium text-muted mb-1">Поставщик:</p>
                      <p class="mb-0">{{ item.supplierSite }}</p>
                      <p *ngIf="item.matchScore !== null" class="small text-muted mt-1">Score: {{ item.matchScore | plainNumber:2 }}</p>
                      <div *ngIf="!isSupplierInCatalog(item.supplierSite)" class="alert alert-warning py-2 small mt-2 mb-0">
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                          <span>Поставщик отсутствует в каталоге.</span>
                          <button class="btn btn-link btn-sm p-0 text-warning" (click)="openAddSupplierForm(item)">Добавить в каталог</button>
                        </div>
                      </div>
                      <div *ngIf="addSupplierItemId === item.id" class="mt-2 p-2 rounded border bg-white">
                        <p class="small fw-medium text-muted mb-2">Добавить поставщика:</p>
                        <div class="mb-2">
                          <input type="text" class="form-control form-control-sm" [(ngModel)]="addSupplierForm.name" placeholder="Название *" required>
                        </div>
                        <div class="mb-2">
                          <input type="text" class="form-control form-control-sm" [(ngModel)]="addSupplierForm.siteUrl" placeholder="Сайт *" required>
                        </div>
                        <div class="mb-2">
                          <input type="email" class="form-control form-control-sm" [(ngModel)]="addSupplierForm.email" placeholder="Email *" required>
                        </div>
                        <div class="mb-2">
                          <input type="text" class="form-control form-control-sm" [(ngModel)]="addSupplierForm.phone" placeholder="Телефон *" required>
                        </div>
                        <div class="mb-2">
                          <input type="text" class="form-control form-control-sm" [(ngModel)]="addSupplierForm.contactPerson" placeholder="Контактное лицо *" required>
                        </div>
                        <div class="d-flex gap-2">
                          <button class="btn btn-primary btn-sm" (click)="saveSupplier(item)" [disabled]="!addSupplierForm.name?.trim() || !addSupplierForm.siteUrl?.trim() || !addSupplierForm.email?.trim() || !addSupplierForm.phone?.trim() || !addSupplierForm.contactPerson?.trim()">Добавить</button>
                          <button class="btn btn-outline-secondary btn-sm" (click)="cancelAddSupplier()">Отмена</button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <!-- Альтернативные варианты -->
                  <div *ngIf="item.foundModels.length" class="mt-3">
                    <p class="small fw-medium text-info mb-2">Альтернативные варианты (топ-{{ item.foundModels.length }}):</p>
                    <div class="list-group">
                      <ng-container *ngFor="let model of item.foundModels">
                        <div class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                          <div class="flex-grow-1" style="min-width: 0; cursor: pointer;" (click)="selectAlternative(item, model)">
                            <div class="fw-medium">{{ model.productName }}</div>
                            <div class="small text-muted">{{ model.supplierSite }} — <a [href]="model.productUrl" target="_blank" class="text-primary" (click)="$event.stopPropagation()">открыть ↗</a></div>
                          </div>
                          <div class="text-end ms-2" style="cursor: pointer;" (click)="selectAlternative(item, model)">
                            <div class="fw-semibold">{{ model.price | plainNumber:0 }} руб.</div>
                            <div *ngIf="model.priceByn" class="small text-success fw-medium">{{ model.priceByn | plainNumber:2 }} BYN</div>
                            <div *ngIf="model.exchangeRate" class="small text-muted">курс {{ model.exchangeRate | plainNumber:4 }}</div>
                            <div class="small" [ngClass]="model.matchScore >= 0.7 ? 'text-success' : model.matchScore >= 0.4 ? 'text-warning' : 'text-muted'">score: {{ model.matchScore | plainNumber:2 }}</div>
                          </div>
                          <div class="d-flex flex-column align-items-center ms-2">
                            <button type="button" class="btn btn-link btn-sm text-info p-0" style="text-decoration: none; line-height: 1;" (click)="toggleEmails(model); $event.stopPropagation();" title="Переписка">
                              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
                            </button>
                            <button type="button" class="btn btn-link btn-sm text-danger p-0" style="text-decoration: none; line-height: 1; font-size: 18px;" (click)="deleteFoundModel(item, model, $event)" title="Удалить">
                              ×
                            </button>
                          </div>
                        </div>
                        <div *ngIf="model.showEmails" class="list-group-item p-2 bg-white">
                          <p class="small fw-medium text-muted mb-2">История переписки</p>
                          <div *ngIf="model.loadingEmails" class="spinner-border spinner-border-sm"></div>
                          <div *ngIf="!model.loadingEmails && !model.emails?.length" class="text-muted small">Нет сообщений</div>
                          <div *ngFor="let email of model.emails" class="mb-2 p-2 rounded small" [class.bg-light]="email.direction === 'OUT'" [class.border-start]="email.direction === 'IN'" [class.border-info]="email.direction === 'IN'" style="border-left-width: 4px !important;">
                            <div class="d-flex justify-content-between">
                              <span class="fw-medium" [class.text-primary]="email.direction === 'OUT'" [class.text-info]="email.direction === 'IN'">{{ email.direction === 'OUT' ? 'Исходящее' : 'Входящее' }}</span>
                              <span class="text-muted small">{{ email.createdAt | date:'dd.MM.yyyy HH:mm' }}</span>
                            </div>
                            <div class="fw-medium">{{ email.subject }}</div>
                            <div class="text-muted" style="white-space: pre-wrap;" [innerHTML]="email.body"></div>
                            <div *ngIf="email.status === 'FAILED'" class="text-danger small">Ошибка: {{ email.errorMessage }}</div>
                          </div>
                          <div class="mt-2">
                            <input type="email" class="form-control form-control-sm mb-2" [(ngModel)]="model.supplierEmail" placeholder="Email поставщика">
                            <input type="text" class="form-control form-control-sm mb-2" [(ngModel)]="model.emailSubject" placeholder="Тема">
                            <textarea class="form-control form-control-sm mb-2" [(ngModel)]="model.emailBody" rows="3" placeholder="Текст сообщения"></textarea>
                            <div class="d-flex gap-2">
                              <button class="btn btn-primary btn-sm" (click)="sendEmail(model)" [disabled]="!model.supplierEmail?.trim() || !model.emailSubject?.trim() || !model.emailBody?.trim()">Отправить</button>
                              <button class="btn btn-outline-secondary btn-sm" (click)="fetchIncomingEmails(model)">Проверить входящие</button>
                            </div>
                          </div>
                        </div>
                      </ng-container>
                    </div>
                    <p class="small text-muted mt-1">Нажмите на вариант, чтобы выбрать его в качестве основного. Нажмите ×, чтобы удалить вариант.</p>
                  </div>
                  <div *ngIf="!item.foundModelName && item.status === 'NOT_FOUND'" class="alert alert-warning py-2 small mb-2">
                    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                      <span>Товар не найден у поставщиков. Попробуйте изменить характеристики и повторить поиск.</span>
                      <button class="btn btn-link btn-sm p-0 text-warning" (click)="startEditParams(item)">Изменить характеристики</button>
                    </div>
                  </div>
                  <div *ngIf="item.status === 'SEARCHING'" class="alert alert-info py-2 small mb-0 d-flex align-items-center gap-2">
                    <span class="spinner-border spinner-border-sm"></span> Поиск...
                  </div>
                  <!-- Ручное добавление варианта -->
                  <div class="mt-3 p-2 rounded border">
                    <p class="small fw-medium text-muted mb-2">Не нашли подходящий вариант? Добавьте вручную:</p>
                    <div class="d-flex gap-2 mb-2">
                      <input type="text" class="form-control form-control-sm" [(ngModel)]="manualModelUrl" placeholder="Ссылка на товар у поставщика (https://...)" name="manualModelUrl">
                      <input type="number" class="form-control form-control-sm" [(ngModel)]="manualModelPrice" placeholder="Стоимость RUB с НДС" name="manualModelPrice" min="0" step="0.01">
                      <button class="btn btn-outline-primary btn-sm" (click)="addManualFoundModel(item)" [disabled]="!manualModelUrl.trim()">Добавить</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Дополнительная информация о найденных моделях -->
        <div *ngFor="let item of items" class="card mb-3" [class.d-none]="!item.foundModelName">
          <div class="card-body">
            <div class="d-flex align-items-center gap-2 mb-2">
              <span class="text-muted small">Лот {{ item.lotNumber }}</span>
              <span *ngIf="item.foundModelName" class="badge bg-success">Найдено у поставщика</span>
            </div>
            <div class="row g-3">
              <div class="col-md-6">
                <p class="small fw-medium text-success mb-1">Найденная модель:</p>
                <p class="mb-1">{{ item.foundModelName }}</p>
                <table class="table table-bordered table-sm mt-2 mb-2" style="max-width: 700px;">
                  <thead class="table-light">
                    <tr>
                      <th>Цена RUR</th>
                      <th>Курс НБ РБ: 1 RUR</th>
                      <th>Доставка BYN</th>
                      <th>Надбавка %</th>
                      <th>Цена BYN</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>{{ item.foundModelPrice | plainNumber:0 }}</td>
                      <td>{{ item.foundModelExchangeRate | plainNumber:4 }}</td>
                      <td>{{ item.deliveryCostByn | plainNumber:2 }}</td>
                      <td>{{ item.markupPercent | plainNumber:1 }}</td>
                      <td><strong class="text-success">{{ item.finalPriceByn | plainNumber:2 }}</strong></td>
                    </tr>
                  </tbody>
                </table>
                <a *ngIf="item.foundModelUrl" [href]="item.foundModelUrl" target="_blank" class="small text-primary">Открыть на сайте поставщика →</a>
              </div>
              <div class="col-md-6" *ngIf="item.supplierSite">
                <p class="small fw-medium text-muted mb-1">Поставщик:</p>
                <p class="mb-0">{{ item.supplierSite }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="activeTab === 'logs'">
        <div class="card">
          <div class="card-body">
            <div *ngIf="logs.length === 0" class="text-center text-muted py-5">Нет записей</div>
            <div class="overflow-auto" style="max-height: 500px;">
              <div *ngFor="let log of logs" class="p-3 rounded mb-2" style="background-color: #f8f9fa;">
                <div class="d-flex align-items-center gap-2 flex-wrap">
                  <span class="badge" [ngClass]="'log-' + log.level.toLowerCase()">{{ log.level }}</span>
                  <span class="fw-medium small">{{ log.step }}</span>
                  <span class="text-muted small ms-auto">{{ log.createdAt | date:'dd.MM.yyyy HH:mm:ss' }}</span>
                </div>
                <p class="mb-0 mt-1">{{ log.message }}</p>
                <p *ngIf="log.details" class="small text-muted mb-0 mt-1">{{ log.details }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class TenderDetailComponent implements OnInit {
  tender: Tender | null = null;
  items: TenderItem[] = [];
  logs: LogEntry[] = [];
  activeTab = 'items';
  STATUS_LABELS = STATUS_LABELS;
  STATUS_CLASSES = STATUS_CLASSES;
  infoCards: { label: string; value: string; icon: string }[] = [];
  editingItemId: number | null = null;
  editText = '';
  editingParamsItemId: number | null = null;
  extractingParamsItemId: number | null = null;
  searchingItemId: number | null = null;
  manualModelUrl: string = '';
  manualModelPrice: number | null = null;
  suppliers: Supplier[] = [];
  addSupplierItemId: number | null = null;
  addSupplierForm: Partial<Supplier> = {};
  private paramEditsCache: Map<number, { key: string; label: string; value: string }[]> = new Map();

  private applyItemDefaults(item: TenderItem) {
    item.markupPercent = item.markupPercent ?? 15;
    item.deliveryCostByn = item.deliveryCostByn ?? 0;
  }

  constructor(
    private route: ActivatedRoute,
    private tenderService: TenderService
  ) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;
    this.tenderService.getTender(id).subscribe(t => {
      this.tender = t;
      this.updateInfoCards();
    });
    this.tenderService.getTenderItems(id).subscribe(i => {
      // Preserve unsaved description edits during manual refresh
      const preserved = new Map<number, string>();
      for (const item of this.items) {
        if (item.documentDescription) preserved.set(item.id, item.documentDescription);
      }
      this.items = i;
      for (const item of this.items) {
        if (preserved.has(item.id)) {
          item.documentDescription = preserved.get(item.id)!;
        }
        this.applyItemDefaults(item);
      }
    });
    this.tenderService.getTenderLogs(id).subscribe(l => this.logs = l);
    this.tenderService.getSuppliers().subscribe(s => this.suppliers = s);
  }

  updateInfoCards() {
    if (!this.tender) return;
    this.infoCards = [
      { label: 'Заказчик', value: this.tender.organizer, icon: '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>' },
      { label: 'Сумма', value: `${this.tender.totalAmount ? Number(this.tender.totalAmount).toFixed(0) : '-'} ${this.tender.currency || ''}`, icon: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline>' },
      { label: 'Позиций', value: String(this.items.length), icon: '<circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>' },
      { label: 'Email отправлено', value: String(this.items.filter(i => i.status === 'EMAIL_SENT').length), icon: '<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline>' },
    ];
  }

  reprocess() {
    if (!this.tender) return;
    this.tenderService.reprocessTender(this.tender.id).subscribe(() => this.loadData());
  }

  startEdit(item: TenderItem) {
    this.editingItemId = item.id;
    this.editText = item.documentDescription || '';
  }

  cancelEdit(item: TenderItem) {
    this.editingItemId = null;
    this.editText = '';
    this.loadData();
  }

  saveEdit(item: TenderItem) {
    if (!this.tender) return;
    this.tenderService.updateItemDocumentDescription(this.tender.id, item.id, item.documentDescription || '').subscribe(updated => {
      item.documentDescription = updated.documentDescription;
    });
  }

  searchByDescription(item: TenderItem) {
    if (!this.tender) return;
    this.searchingItemId = item.id;
    item.status = 'SEARCHING';
    this.tenderService.searchSuppliersByDescription(this.tender.id, item.id).subscribe({
      next: (updated) => {
        this.searchingItemId = null;
        item.foundModelName = updated.foundModelName;
        item.foundModelUrl = updated.foundModelUrl;
        item.foundModelPrice = updated.foundModelPrice;
        item.foundModelPriceByn = updated.foundModelPriceByn;
        item.foundModelExchangeRate = updated.foundModelExchangeRate;
        item.deliveryCostByn = updated.deliveryCostByn;
        item.markupPercent = updated.markupPercent;
        item.finalPriceByn = updated.finalPriceByn;
        item.supplierSite = updated.supplierSite;
        item.matchScore = updated.matchScore;
        item.foundModels = updated.foundModels || [];
        this.applyItemDefaults(item);
        item.status = updated.status;
      },
      error: () => {
        this.searchingItemId = null;
        item.status = 'NOT_FOUND';
      }
    });
  }

  selectAlternative(item: TenderItem, model: FoundModel) {
    item.foundModelName = model.productName;
    item.foundModelUrl = model.productUrl;
    item.foundModelPrice = model.price;
    item.foundModelPriceByn = model.priceByn;
    item.foundModelExchangeRate = model.exchangeRate;
    item.supplierSite = model.supplierSite;
    item.matchScore = model.matchScore;
    item.selectedFoundModelId = model.id;
    this.applyItemDefaults(item);
  }

  saveSelectedModel(item: TenderItem) {
    if (!this.tender || !item.selectedFoundModelId) return;
    this.tenderService.selectFoundModel(this.tender.id, item.id, item.selectedFoundModelId).subscribe({
      next: (updated) => {
        item.foundModelName = updated.foundModelName;
        item.foundModelUrl = updated.foundModelUrl;
        item.foundModelPrice = updated.foundModelPrice;
        item.foundModelPriceByn = updated.foundModelPriceByn;
        item.foundModelExchangeRate = updated.foundModelExchangeRate;
        item.deliveryCostByn = updated.deliveryCostByn;
        item.markupPercent = updated.markupPercent;
        item.finalPriceByn = updated.finalPriceByn;
        item.supplierSite = updated.supplierSite;
        item.matchScore = updated.matchScore;
        item.foundModels = updated.foundModels || [];
        item.status = updated.status;
        item.selectedFoundModelId = null;
        this.applyItemDefaults(item);
      }
    });
  }

  getCurrentFoundModel(item: TenderItem): FoundModel | null {
    if (!item.foundModelUrl || !item.foundModels) return null;
    return item.foundModels.find(m => m.productUrl === item.foundModelUrl) || null;
  }

  private normalizeSupplierHost(site: string | null | undefined): string {
    if (!site) return '';
    let url = site.trim().toLowerCase();
    url = url.replace(/^https?:\/\//, '');
    url = url.replace(/^www\./, '');
    url = url.replace(/\/.*$/, '');
    return url;
  }

  isSupplierInCatalog(supplierSite: string | null | undefined): boolean {
    if (!supplierSite) return false;
    const host = this.normalizeSupplierHost(supplierSite);
    return this.suppliers.some(s => this.normalizeSupplierHost(s.siteUrl) === host);
  }

  openAddSupplierForm(item: TenderItem) {
    this.addSupplierItemId = item.id;
    const site = item.supplierSite || '';
    this.addSupplierForm = {
      name: '',
      siteUrl: site.startsWith('http') ? site : `https://${site}`,
      email: '',
      phone: '',
      contactPerson: ''
    };
  }

  cancelAddSupplier() {
    this.addSupplierItemId = null;
    this.addSupplierForm = {};
  }

  saveSupplier(item: TenderItem) {
    const data = { ...this.addSupplierForm, isActive: true };
    this.tenderService.createSupplier(data).subscribe({
      next: (created) => {
        this.suppliers.push(created);
        this.addSupplierItemId = null;
        this.addSupplierForm = {};
      }
    });
  }

  toggleEmails(model: FoundModel) {
    model.showEmails = !model.showEmails;
    if (model.showEmails) {
      this.fetchIncomingEmails(model);
    }
  }

  loadEmails(model: FoundModel) {
    model.loadingEmails = true;
    this.tenderService.getFoundModelEmails(model.id).subscribe({
      next: (emails) => {
        model.emails = emails.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        model.loadingEmails = false;
      },
      error: () => {
        model.loadingEmails = false;
      }
    });
    if (!model.supplierEmail && model.supplierEmail !== '') {
      this.tenderService.resolveSupplierEmail(model.id).subscribe({
        next: (email) => model.supplierEmail = email || ''
      });
    }
    if (!model.emailSubject?.trim()) {
      model.emailSubject = 'Запрос возможности поставки оборудования для участия в тендере РБ';
    }
    if (!model.emailBody?.trim()) {
      model.emailBody = `Здравствуйте.\nПросим вас сообщить о возможности поставки оборудования (сроки поставки, стоимость) для участия в тендере на территории РБ.\nИнформация о запрашиваемой модели:\nСсылка: ${model.productUrl || ''}\nКоличество: \nЕсли данная модель не доступна предложите пожалуйста аналог`;
    }
  }

  sendEmail(model: FoundModel) {
    if (!model.supplierEmail?.trim() || !model.emailSubject?.trim() || !model.emailBody?.trim()) return;
    this.tenderService.sendFoundModelEmail(model.id, {
      toEmail: model.supplierEmail.trim(),
      subject: model.emailSubject.trim(),
      body: model.emailBody.trim()
    }).subscribe({
      next: (sent) => {
        model.emails = model.emails || [];
        model.emails.unshift(sent);
        model.emailSubject = '';
        model.emailBody = '';
      }
    });
  }

  fetchIncomingEmails(model: FoundModel) {
    model.loadingEmails = true;
    this.tenderService.fetchIncomingFoundModelEmails().subscribe({
      next: () => this.loadEmails(model),
      error: () => this.loadEmails(model)
    });
  }

  updatePricing(item: TenderItem) {
    if (!this.tender) return;
    const delivery = item.deliveryCostByn || 0;
    const markup = item.markupPercent || 0;
    this.tenderService.updatePricing(this.tender.id, item.id, {
      deliveryCostByn: delivery,
      markupPercent: markup
    }).subscribe({
      next: (updated) => {
        item.deliveryCostByn = updated.deliveryCostByn;
        item.markupPercent = updated.markupPercent;
        item.finalPriceByn = updated.finalPriceByn;
      }
    });
  }

  deleteFoundModel(item: TenderItem, model: FoundModel, event: MouseEvent) {
    event.stopPropagation();
    if (!this.tender) return;
    this.tenderService.deleteFoundModel(this.tender.id, item.id, model.id).subscribe({
      next: (updated) => {
        item.foundModelName = updated.foundModelName;
        item.foundModelUrl = updated.foundModelUrl;
        item.foundModelPrice = updated.foundModelPrice;
        item.foundModelPriceByn = updated.foundModelPriceByn;
        item.foundModelExchangeRate = updated.foundModelExchangeRate;
        item.deliveryCostByn = updated.deliveryCostByn;
        item.markupPercent = updated.markupPercent;
        item.finalPriceByn = updated.finalPriceByn;
        item.supplierSite = updated.supplierSite;
        item.matchScore = updated.matchScore;
        item.foundModels = updated.foundModels || [];
        this.applyItemDefaults(item);
        item.status = updated.status;
      }
    });
  }

  addManualFoundModel(item: TenderItem) {
    if (!this.tender || !this.manualModelUrl.trim()) return;
    this.tenderService.addFoundModel(this.tender.id, item.id, {
      productUrl: this.manualModelUrl.trim(),
      price: this.manualModelPrice !== null && this.manualModelPrice > 0 ? this.manualModelPrice : undefined
    }).subscribe({
      next: (updated) => {
        item.foundModelName = updated.foundModelName;
        item.foundModelUrl = updated.foundModelUrl;
        item.foundModelPrice = updated.foundModelPrice;
        item.foundModelPriceByn = updated.foundModelPriceByn;
        item.foundModelExchangeRate = updated.foundModelExchangeRate;
        item.deliveryCostByn = updated.deliveryCostByn;
        item.markupPercent = updated.markupPercent;
        item.finalPriceByn = updated.finalPriceByn;
        item.supplierSite = updated.supplierSite;
        item.matchScore = updated.matchScore;
        item.foundModels = updated.foundModels || [];
        this.applyItemDefaults(item);
        item.status = updated.status;
        this.manualModelUrl = '';
        this.manualModelPrice = null;
      }
    });
  }

  extractParameters(item: TenderItem) {
    if (!this.tender) return;
    this.extractingParamsItemId = item.id;
    this.tenderService.extractParameters(this.tender.id, item.id).subscribe({
      next: (updated) => {
        item.extractedParams = updated.extractedParams;
        this.extractingParamsItemId = null;
      },
      error: () => {
        this.extractingParamsItemId = null;
      }
    });
  }

  startEditParams(item: TenderItem) {
    this.editingParamsItemId = item.id;
    const params = this.parseParamList(item.extractedParams);
    const edits = params.map(p => ({ key: p.key, label: p.label, value: String(p.value) }));
    this.paramEditsCache.set(item.id, edits);
  }

  getParamEdits(itemId: number): { key: string; label: string; value: string }[] {
    return this.paramEditsCache.get(itemId) || [];
  }

  addParamEdit(itemId: number) {
    const edits = this.paramEditsCache.get(itemId) || [];
    edits.push({ key: '', label: '', value: '' });
    this.paramEditsCache.set(itemId, edits);
  }

  removeParamEdit(itemId: number, index: number) {
    const edits = this.paramEditsCache.get(itemId) || [];
    edits.splice(index, 1);
    this.paramEditsCache.set(itemId, edits);
  }

  cancelEditParams(item: TenderItem) {
    this.editingParamsItemId = null;
    this.paramEditsCache.delete(item.id);
  }

  saveParams(item: TenderItem) {
    if (!this.tender) return;
    const edits = this.paramEditsCache.get(item.id) || [];
    const params: Record<string, any> = {};
    for (const p of edits) {
      if (p.key.trim()) {
        params[p.key.trim()] = {
          label: p.label.trim() || p.key.trim(),
          value: p.value
        };
      }
    }
    this.tenderService.updateExtractedParams(this.tender.id, item.id, params).subscribe({
      next: (updated) => {
        item.extractedParams = updated.extractedParams;
        this.editingParamsItemId = null;
        this.paramEditsCache.delete(item.id);
      }
    });
  }

  deleteParam(item: TenderItem, key: string) {
    if (!this.tender) return;
    this.tenderService.deleteExtractedParam(this.tender.id, item.id, key).subscribe({
      next: (updated) => {
        item.extractedParams = updated.extractedParams;
      }
    });
  }

  parseParamList(json: string | null): { key: string; label: string; value: any }[] {
    if (!json) return [];
    try {
      const obj = JSON.parse(json);
      return Object.entries(obj).map(([k, v]) => {
        if (v !== null && typeof v === 'object' && 'value' in v) {
          return { key: k, label: String((v as any).label || k), value: (v as any).value };
        }
        return { key: k, label: k, value: v };
      });
    } catch {
      return [];
    }
  }

  parseParams(json: string | null): Record<string, any> {
    if (!json) return {};
    try {
      return JSON.parse(json);
    } catch {
      return {};
    }
  }
}
