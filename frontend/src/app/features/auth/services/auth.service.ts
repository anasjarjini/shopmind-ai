import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError, throwError } from 'rxjs';
import { StorageService } from '../../../core/services/storage.service';
import { NotificationService } from '../../../core/services/notification.service';
import { User, AuthResponse, LoginRequest, RegisterRequest } from '../../../shared/models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http         = inject(HttpClient);
  private readonly storage      = inject(StorageService);
  private readonly router       = inject(Router);
  private readonly notification = inject(NotificationService);

  /** JWT access token (from localStorage). */
  readonly token       = signal<string | null>(this.storage.get<string>('access_token'));
  /** Currently authenticated user. */
  readonly currentUser = signal<User | null>(null);
  /** True when a valid token is present. */
  readonly isLoggedIn  = computed(() => !!this.token());

  constructor() {
    // On app init, if a token exists, verify it with /me
    if (this.token()) {
      this.loadMe();
    }
  }

  // ─── Public API ─────────────────────────────────────────────────────────────

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap(res => this.applyAuth(res)),
      catchError(err => {
        const msg = this.extractError(err, 'Invalid email or password.');
        this.notification.error(msg);
        return throwError(() => err);
      })
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap(res => this.applyAuth(res)),
      catchError(err => {
        const msg = this.extractError(err, 'Registration failed. Please try again.');
        this.notification.error(msg);
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    // fire-and-forget: clear client-side immediately
    this.http.post('/api/auth/logout', {}).subscribe();
    this.clearAuth();
    this.notification.info('You have been logged out.');
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.http.put<{ message: string }>('/api/users/password', { currentPassword, newPassword }).pipe(
      tap(() => this.notification.success('Password changed successfully.')),
      catchError(err => {
        const msg = this.extractError(err, 'Could not change password.');
        this.notification.error(msg);
        return throwError(() => err);
      })
    );
  }

  loadMe(): void {
    this.http.get<User>('/api/auth/me').subscribe({
      next: user => this.currentUser.set(user),
      error: () => this.clearAuth()
    });
  }

  // ─── Private helpers ────────────────────────────────────────────────────────

  private extractError(err: any, fallback: string): string {
    if (!err.error) return fallback;                          // Network error
    if (typeof err.error === 'string') return err.error;     // Plain text
    if (err.error.details && typeof err.error.details === 'object') {
      const firstDetail = Object.values(err.error.details)[0];
      if (typeof firstDetail === 'string') return firstDetail;
    }
    return err.error.message ?? err.error.error ?? fallback; // JSON body
  }

  private applyAuth(res: AuthResponse): void {
    this.storage.set('access_token', res.token);
    this.token.set(res.token);
    this.currentUser.set({
      id: '',                              // updated by loadMe()
      fullName: res.fullName,
      email:    res.email,
      role:     res.role as 'USER' | 'ADMIN'
    });
    this.loadMe();
  }

  private clearAuth(): void {
    this.storage.remove('access_token');
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/']);
  }
}
