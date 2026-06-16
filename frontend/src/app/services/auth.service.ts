import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { User } from '../models/models';
import { Router } from '@angular/router';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = '/api';
  private currentUser$ = new BehaviorSubject<User | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromStorage();
  }

  login(email: string, password: string): Observable<AuthTokens & { user: User }> {
    return this.http.post<{ accessToken: string; refreshToken: string; user: User }>(
      `${this.apiUrl}/auth/login`, { email, password }
    ).pipe(
      tap(response => this.setSession(response)),
      catchError(err => {
        const msg = err.error?.message || 'Неверный email или пароль';
        return throwError(() => new Error(msg));
      })
    );
  }

  register(email: string, password: string, fullName?: string): Observable<AuthTokens & { user: User }> {
    return this.http.post<{ accessToken: string; refreshToken: string; user: User }>(
      `${this.apiUrl}/auth/register`, { email, password, fullName }
    ).pipe(
      tap(response => this.setSession(response)),
      catchError(err => {
        const msg = err.error?.message || 'Ошибка регистрации';
        return throwError(() => new Error(msg));
      })
    );
  }

  refresh(refreshToken: string): Observable<AuthTokens & { user: User }> {
    return this.http.post<{ accessToken: string; refreshToken: string; user: User }>(
      `${this.apiUrl}/auth/refresh`, { refreshToken }
    ).pipe(
      tap(response => this.setSession(response)),
      catchError(err => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http.post(`${this.apiUrl}/auth/logout`, { refreshToken }).subscribe({
        error: () => {}
      });
    }
    this.clearSession();
    this.router.navigate(['/login']);
  }

  getCurrentUser(): Observable<User | null> {
    if (this.currentUser$.value) {
      return this.currentUser$.asObservable();
    }
    const token = this.getAccessToken();
    if (!token) {
      return of(null);
    }
    return this.http.get<User>(`${this.apiUrl}/users/me`).pipe(
      tap(user => this.currentUser$.next(user)),
      catchError(() => {
        this.clearSession();
        return of(null);
      })
    );
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }

  private setSession(response: AuthTokens & { user: User }): void {
    localStorage.setItem('access_token', response.accessToken);
    localStorage.setItem('refresh_token', response.refreshToken);
    this.currentUser$.next(response.user);
  }

  private clearSession(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    this.currentUser$.next(null);
  }

  private loadUserFromStorage(): void {
    if (this.isLoggedIn()) {
      this.getCurrentUser().subscribe();
    }
  }
}
