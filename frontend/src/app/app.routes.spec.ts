import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { AuthStore } from './core/auth/auth.store';
import { Toaster } from './core/notification/toaster';

describe('routes', () => {
    const isAuthenticated = signal(false);
    let harness: RouterTestingHarness;

    const urlAfter = async (url: string) => {
        await harness.navigateByUrl(url);
        return TestBed.inject(Router).url;
    };

    beforeEach(async () => {
        isAuthenticated.set(false);
        TestBed.configureTestingModule({
            providers: [provideRouter(routes), { provide: AuthStore, useValue: { isAuthenticated } }],
        });
        harness = await RouterTestingHarness.create();
    });

    it('should redirect the root to the products page', async () => {
        expect(await urlAfter('/')).toBe('/products');
    });

    it('should open the products page to a guest', async () => {
        expect(await urlAfter('/products')).toBe('/products');
    });

    it('should send a guest from the cart to the login with the return url', async () => {
        expect(await urlAfter('/cart')).toBe('/login?returnUrl=%2Fcart');
    });

    it('should open the cart to a logged-in user', async () => {
        isAuthenticated.set(true);

        expect(await urlAfter('/cart')).toBe('/cart');
    });

    it('should open the login to a guest', async () => {
        expect(await urlAfter('/login')).toBe('/login');
    });

    it('should send a logged-in user from the login to the products page', async () => {
        isAuthenticated.set(true);

        expect(await urlAfter('/login')).toBe('/products');
    });

    it('should send an unknown route to the products page with a notice', async () => {
        expect(await urlAfter('/nao-existe')).toBe('/products');
        expect(TestBed.inject(Toaster).toasts()[0].message).toBe('Página não encontrada');
    });
});