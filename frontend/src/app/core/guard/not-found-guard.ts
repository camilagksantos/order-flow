import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Toaster } from '../notification/toaster';

export const notFoundGuard: CanActivateFn = () => {
  inject(Toaster).info('Página não encontrada');
  return inject(Router).createUrlTree(['/products']);
};