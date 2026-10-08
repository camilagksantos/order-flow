import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { guestGuard } from './guest-guard';
import { AuthStore } from '../auth.store';


describe('guestGuard', () => {
  const isAuthenticated = signal(false);
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/login' } as RouterStateSnapshot;

  beforeEach(() => {
    isAuthenticated.set(false);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthStore, useValue: { isAuthenticated } }],
    });
  });

  it('should allow a guest', () => {
    const result = TestBed.runInInjectionContext(() => guestGuard(route, state));

    expect(result).toBe(true);
  });

  it('should redirect a logged-in user to the catalog', () => {
    isAuthenticated.set(true);
    const router = TestBed.inject(Router);

    const result = TestBed.runInInjectionContext(() => guestGuard(route, state)) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/products');
  });
});