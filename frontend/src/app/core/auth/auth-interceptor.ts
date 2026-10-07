import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthStore } from './auth.store';

const apiPrefix = `${environment.apiUrl}/api/`;
const authPrefix = `${apiPrefix}v1/auth/`;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const accessToken = inject(AuthStore).accessToken();
  const isApiCall = req.url.startsWith(apiPrefix);
  const isAuthRoute = req.url.startsWith(authPrefix);

  if (!accessToken || !isApiCall || isAuthRoute) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } }));
};