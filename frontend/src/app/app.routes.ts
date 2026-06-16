import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { TenderDetailComponent } from './pages/tender-detail/tender-detail.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: '', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: 'tenders/:id', component: TenderDetailComponent, canActivate: [authGuard] },
  { path: 'suppliers', loadComponent: () => import('./pages/suppliers/suppliers.component').then(m => m.SuppliersComponent), canActivate: [authGuard] },
  { path: 'logs', loadComponent: () => import('./pages/logs/logs.component').then(m => m.LogsComponent), canActivate: [authGuard] },
  { path: 'config', loadComponent: () => import('./pages/config/config.component').then(m => m.ConfigComponent), canActivate: [authGuard] },
  { path: 'users', loadComponent: () => import('./pages/users/users.component').then(m => m.UsersComponent), canActivate: [authGuard] },
  { path: 'equipment-catalog', loadComponent: () => import('./pages/equipment-catalog/equipment-catalog.component').then(m => m.EquipmentCatalogComponent), canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
