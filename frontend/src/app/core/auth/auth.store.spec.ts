import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthApi } from './auth-api';
import { AuthStore } from './auth.store';

function buildToken(payload: object): string {
    const encode = (value: object) =>
        btoa(JSON.stringify(value)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(payload)}.signature`;
}

describe('AuthStore', () => {
    const authApi = { login: vi.fn(), refresh: vi.fn() };
    let store: InstanceType<typeof AuthStore>;

    beforeEach(() => {
        authApi.login.mockReset();
        authApi.refresh.mockReset();
        TestBed.configureTestingModule({
            providers: [{ provide: AuthApi, useValue: authApi }],
        });
        store = TestBed.inject(AuthStore);
    });

    it('should start without a session', () => {
        expect(store.isAuthenticated()).toBe(false);
        expect(store.user()).toBeNull();
        expect(store.customerId()).toBeNull();
    });

    it('should store the token and the customer data after a customer login', async () => {
        const accessToken = buildToken({
            sub: 'ana@exemplo.pt',
            roles: ['ROLE_CUSTOMER'],
            customerId: 7,
            type: 'access',
        });
        authApi.login.mockReturnValue(of({ accessToken, tokenType: 'Bearer', expiresIn: 900 }));

        await store.login({ email: 'ana@exemplo.pt', password: 'Password123' });

        expect(store.isAuthenticated()).toBe(true);
        expect(store.accessToken()).toBe(accessToken);
        expect(store.user()).toEqual({ email: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7 });
        expect(store.customerId()).toBe(7);
        expect(store.isAdmin()).toBe(false);
    });

    it('should recognise an admin, who has no customer id', async () => {
        const accessToken = buildToken({ sub: 'admin@exemplo.pt', roles: ['ROLE_ADMIN'], type: 'access' });
        authApi.login.mockReturnValue(of({ accessToken, tokenType: 'Bearer', expiresIn: 900 }));

        await store.login({ email: 'admin@exemplo.pt', password: 'Password123' });

        expect(store.isAdmin()).toBe(true);
        expect(store.customerId()).toBeNull();
    });

    it('should keep the session empty when the login fails', async () => {
        authApi.login.mockReturnValue(throwError(() => new Error('401')));

        await expect(store.login({ email: 'ana@exemplo.pt', password: 'wrong' })).rejects.toThrow('401');

        expect(store.isAuthenticated()).toBe(false);
        expect(store.user()).toBeNull();
    });

    it('should replace the session with the new token on refresh', async () => {
        const oldToken = buildToken({ sub: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7, type: 'access', iat: 1 });
        const newToken = buildToken({ sub: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7, type: 'access', iat: 2 });
        authApi.login.mockReturnValue(of({ accessToken: oldToken, tokenType: 'Bearer', expiresIn: 900 }));
        authApi.refresh.mockReturnValue(of({ accessToken: newToken, tokenType: 'Bearer', expiresIn: 900 }));
        await store.login({ email: 'ana@exemplo.pt', password: 'Password123' });

        await store.refresh();

        expect(store.accessToken()).toBe(newToken);
        expect(store.customerId()).toBe(7);
    });

    it('should clear the session and rethrow when the refresh fails', async () => {
        const accessToken = buildToken({ sub: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7, type: 'access' });
        authApi.login.mockReturnValue(of({ accessToken, tokenType: 'Bearer', expiresIn: 900 }));
        authApi.refresh.mockReturnValue(throwError(() => new Error('401')));
        await store.login({ email: 'ana@exemplo.pt', password: 'Password123' });

        await expect(store.refresh()).rejects.toThrow('401');

        expect(store.isAuthenticated()).toBe(false);
        expect(store.user()).toBeNull();
    });

    it('should share one request between simultaneous refreshes', async () => {
        const newToken = buildToken({ sub: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7, type: 'access' });
        authApi.refresh.mockReturnValue(of({ accessToken: newToken, tokenType: 'Bearer', expiresIn: 900 }));

        await Promise.all([store.refresh(), store.refresh()]);

        expect(authApi.refresh).toHaveBeenCalledTimes(1);
        expect(store.accessToken()).toBe(newToken);
    });

    it('should make a new request when a refresh is asked after the previous one finished', async () => {
        const newToken = buildToken({ sub: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7, type: 'access' });
        authApi.refresh.mockReturnValue(of({ accessToken: newToken, tokenType: 'Bearer', expiresIn: 900 }));

        await store.refresh();
        await store.refresh();

        expect(authApi.refresh).toHaveBeenCalledTimes(2);
    });

    it('should empty the session on clearSession', async () => {
        const accessToken = buildToken({ sub: 'ana@exemplo.pt', roles: ['ROLE_CUSTOMER'], customerId: 7, type: 'access' });
        authApi.login.mockReturnValue(of({ accessToken, tokenType: 'Bearer', expiresIn: 900 }));
        await store.login({ email: 'ana@exemplo.pt', password: 'Password123' });

        store.clearSession();

        expect(store.isAuthenticated()).toBe(false);
        expect(store.user()).toBeNull();
        expect(store.accessToken()).toBeNull();
    });
});