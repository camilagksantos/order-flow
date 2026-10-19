import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Toaster } from '../notification/toaster';
import { notFoundGuard } from './not-found-guard';

describe('notFoundGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/nao-existe' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('should show a notice that the page was not found', () => {
    TestBed.runInInjectionContext(() => notFoundGuard(route, state));

    expect(TestBed.inject(Toaster).toasts()[0].message).toBe('Página não encontrada');
  });

  it('should redirect to the products page', () => {
    const result = TestBed.runInInjectionContext(() => notFoundGuard(route, state)) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/products');
  });
});