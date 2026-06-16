import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EquipmentType, Manufacturer, EquipmentCatalogItem } from '../models/models';

@Injectable({ providedIn: 'root' })
export class EquipmentCatalogService {
  private apiUrl = '/api/equipment-catalog';

  constructor(private http: HttpClient) {}

  getTypes(): Observable<EquipmentType[]> {
    return this.http.get<EquipmentType[]>(`${this.apiUrl}/types`);
  }

  getManufacturers(): Observable<Manufacturer[]> {
    return this.http.get<Manufacturer[]>(`${this.apiUrl}/manufacturers`);
  }

  getItems(typeId?: number, manufacturerId?: number): Observable<EquipmentCatalogItem[]> {
    let url = `${this.apiUrl}/items`;
    const params: string[] = [];
    if (typeId) params.push(`typeId=${typeId}`);
    if (manufacturerId) params.push(`manufacturerId=${manufacturerId}`);
    if (params.length) url += '?' + params.join('&');
    return this.http.get<EquipmentCatalogItem[]>(url);
  }

  getItem(id: number): Observable<EquipmentCatalogItem> {
    return this.http.get<EquipmentCatalogItem>(`${this.apiUrl}/items/${id}`);
  }

  createItem(data: {
    equipmentTypeId: number;
    manufacturerId: number;
    modelName: string;
    modelNumber?: string;
    specValues?: Record<number, string>;
  }): Observable<EquipmentCatalogItem> {
    return this.http.post<EquipmentCatalogItem>(`${this.apiUrl}/items`, data);
  }

  updateItem(id: number, data: {
    equipmentTypeId: number;
    manufacturerId: number;
    modelName: string;
    modelNumber?: string;
    specValues?: Record<number, string>;
  }): Observable<EquipmentCatalogItem> {
    return this.http.put<EquipmentCatalogItem>(`${this.apiUrl}/items/${id}`, data);
  }

  deleteItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${id}`);
  }

  createType(data: { name: string; code: string }): Observable<EquipmentType> {
    return this.http.post<EquipmentType>(`${this.apiUrl}/types`, data);
  }

  deleteType(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/types/${id}`);
  }

  createManufacturer(data: { name: string; country?: string; website?: string }): Observable<Manufacturer> {
    return this.http.post<Manufacturer>(`${this.apiUrl}/manufacturers`, data);
  }

  deleteManufacturer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/manufacturers/${id}`);
  }
}
