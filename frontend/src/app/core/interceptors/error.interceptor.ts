import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { StorageService } from '../services/storage.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notification = inject(NotificationService);
  const storage      = inject(StorageService);
  const router       = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // 0 = network error (CORS block, server offline) — let the feature handle it
      if (err.status === 0) {
        return throwError(() => err);
      }

      // 401 outside of auth endpoints → clear session and redirect
      if (err.status === 401 && !req.url.includes('/api/auth/')) {
        storage.remove('access_token');
        router.navigate(['/auth/login']);
        return throwError(() => err);
      }

      // 403 — only show if NOT a CORS pre-flight (those are status 0 above)
      if (err.status === 403) {
        notification.error('Access denied.');
      }

      // 5xx server errors
      if (err.status >= 500) {
        notification.error('Server error. Please try again later.');
      }

      return throwError(() => err);
    })
  );
};
