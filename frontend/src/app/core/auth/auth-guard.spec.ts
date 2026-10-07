import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth-guard';
import { AuthStore } from './auth.store';

describe('authGuard', () => {
  const isAuthenticated = signal(false);
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/cart' } as RouterStateSnapshot;

  beforeEach(() => {
    isAuthenticated.set(false);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: { isAuthenticated } }],
    });
  });

  it('should allow a logged-in user', () => {
    isAuthenticated.set(true);

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBe(true);
  });

  it('should redirect a guest to the login with the return url', () => {
    const router = TestBed.inject(Router);

    const result = TestBed.runInInjectionContext(() => authGuard(route, state)) as UrlTree;

    expect(router.serializeUrl(result).split('?')[0]).toBe('/login');
    expect(result.queryParams['returnUrl']).toBe('/cart');
  });
});