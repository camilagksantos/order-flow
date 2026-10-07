import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStore } from './auth.store';

const apiPrefix = `${environment.apiUrl}/api/`;
const authPrefix = `${apiPrefix}v1/auth/`;

const withToken = (req: HttpRequest<unknown>, accessToken: string): HttpRequest<unknown> =>
  req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } });

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthStore);
  const accessToken = store.accessToken();
  const isApiCall = req.url.startsWith(apiPrefix);
  const isAuthRoute = req.url.startsWith(authPrefix);

  if (!accessToken || !isApiCall || isAuthRoute) {
    return next(req);
  }

  return next(withToken(req, accessToken)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return from(store.refresh()).pipe(
        switchMap(() => {
          const newToken = store.accessToken();
          return newToken ? next(withToken(req, newToken)) : throwError(() => error);
        })
      );
    })
  );
};