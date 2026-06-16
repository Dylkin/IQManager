import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tender, TenderItem, LogEntry, DashboardStats, FoundModelEmail, Supplier, Config, TelegramMessage, User } from '../models/models';

@Injectable({ providedIn: 'root' })
export class TenderService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  getTenders(): Observable<Tender[]> {
    return this.http.get<Tender[]>(`${this.apiUrl}/tenders`);
  }

  getTender(id: number): Observable<Tender> {
    return this.http.get<Tender>(`${this.apiUrl}/tenders/${id}`);
  }

  getTenderItems(id: number): Observable<TenderItem[]> {
    return this.http.get<TenderItem[]>(`${this.apiUrl}/tenders/${id}/items`);
  }

  getTenderLogs(id: number): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.apiUrl}/tenders/${id}/logs`);
  }

  getTendersByStatus(status: string): Observable<Tender[]> {
    return this.http.get<Tender[]>(`${this.apiUrl}/tenders/status/${status}`);
  }

  processUrl(url: string): Observable<Tender> {
    return this.http.post<Tender>(`${this.apiUrl}/tenders/process-url`, url);
  }

  reprocessTender(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/tenders/${id}/reprocess`, {});
  }

  deleteTender(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/tenders/${id}`);
  }

  updateItemDocumentDescription(tenderId: number, itemId: number, documentDescription: string): Observable<TenderItem> {
    return this.http.put<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/document-description`, {
      documentDescription
    });
  }

  searchSuppliersForItem(tenderId: number, itemId: number): Observable<TenderItem> {
    return this.http.post<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/search-suppliers`, {});
  }

  searchSuppliersByDescription(tenderId: number, itemId: number): Observable<TenderItem> {
    return this.http.post<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/search-by-description`, {});
  }

  extractParameters(tenderId: number, itemId: number): Observable<TenderItem> {
    return this.http.post<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/extract-params`, {});
  }

  updateExtractedParams(tenderId: number, itemId: number, params: Record<string, any>): Observable<TenderItem> {
    return this.http.put<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/extracted-params`, params);
  }

  deleteExtractedParam(tenderId: number, itemId: number, key: string): Observable<TenderItem> {
    return this.http.delete<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/extracted-params/${encodeURIComponent(key)}`);
  }

  deleteFoundModel(tenderId: number, itemId: number, modelId: number): Observable<TenderItem> {
    return this.http.delete<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/found-models/${modelId}`);
  }

  addFoundModel(tenderId: number, itemId: number, data: { productUrl: string; productName?: string; price?: number }): Observable<TenderItem> {
    return this.http.post<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/add-found-model`, data);
  }

  updatePricing(tenderId: number, itemId: number, data: { deliveryCostByn?: number; markupPercent?: number }): Observable<TenderItem> {
    return this.http.post<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/pricing`, data);
  }

  selectFoundModel(tenderId: number, itemId: number, modelId: number): Observable<TenderItem> {
    return this.http.post<TenderItem>(`${this.apiUrl}/tenders/${tenderId}/items/${itemId}/select-found-model/${modelId}`, {});
  }

  getFoundModelEmails(foundModelId: number): Observable<FoundModelEmail[]> {
    return this.http.get<FoundModelEmail[]>(`${this.apiUrl}/found-models/${foundModelId}/emails`);
  }

  resolveSupplierEmail(foundModelId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/found-models/${foundModelId}/supplier-email`, { responseType: 'text' });
  }

  sendFoundModelEmail(foundModelId: number, data: { toEmail: string; subject: string; body: string }): Observable<FoundModelEmail> {
    return this.http.post<FoundModelEmail>(`${this.apiUrl}/found-models/${foundModelId}/emails/send`, data);
  }

  fetchIncomingFoundModelEmails(): Observable<number> {
    return this.http.post<number>(`${this.apiUrl}/found-models/emails/fetch-incoming`, {});
  }

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard/stats`);
  }

  getRecentLogs(): Observable<LogEntry[]> {
    return this.http.get<LogEntry[]>(`${this.apiUrl}/dashboard/logs`);
  }

  getStatusDistribution(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.apiUrl}/dashboard/status-distribution`);
  }

  getSuppliers(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(`${this.apiUrl}/suppliers`);
  }

  getActiveSuppliers(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(`${this.apiUrl}/suppliers/active`);
  }

  createSupplier(data: Partial<Supplier>): Observable<Supplier> {
    return this.http.post<Supplier>(`${this.apiUrl}/suppliers`, data);
  }

  updateSupplier(id: number, data: Partial<Supplier>): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/suppliers/${id}`, data);
  }

  deleteSupplier(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/suppliers/${id}`);
  }

  // Config CRUD
  getConfigs(): Observable<Config[]> {
    return this.http.get<Config[]>(`${this.apiUrl}/configs`);
  }

  getConfigsByGroup(group: string): Observable<Config[]> {
    return this.http.get<Config[]>(`${this.apiUrl}/configs/group/${group}`);
  }

  getConfigGroups(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/configs/groups`);
  }

  getConfigById(id: number): Observable<Config> {
    return this.http.get<Config>(`${this.apiUrl}/configs/${id}`);
  }

  createConfig(data: Partial<Config>): Observable<Config> {
    return this.http.post<Config>(`${this.apiUrl}/configs`, data);
  }

  updateConfig(id: number, data: Partial<Config>): Observable<Config> {
    return this.http.put<Config>(`${this.apiUrl}/configs/${id}`, data);
  }

  deleteConfig(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/configs/${id}`);
  }

  // Telegram
  fetchTodayMessages(): Observable<TelegramMessage[]> {
    return this.http.post<TelegramMessage[]>(`${this.apiUrl}/telegram/fetch-today`, {});
  }

  // Users CRUD (admin)
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }

  createUser(data: Partial<User> & { password: string }): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/users`, data);
  }

  updateUser(id: number, data: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/users/${id}`, data);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${id}`);
  }
}
