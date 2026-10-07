import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth.store';
import { inject } from '@angular/core';

export const guestGuard: CanActivateFn = (route, state) => {
  const store = inject(AuthStore);
  const router = inject(Router);

  if (!store.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/products']);
};
