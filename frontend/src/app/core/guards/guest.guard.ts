import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StorageService } from '../services/storage.service';

export const guestGuard: CanActivateFn = () => {
  const storage = inject(StorageService);
  const router   = inject(Router);

  if (!storage.get<string>('access_token')) {
    return true;
  }
  router.navigate(['/']);
  return false;
};
