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
    const authApi = { login: vi.fn() };
    let store: InstanceType<typeof AuthStore>;

    beforeEach(() => {
        authApi.login.mockReset();
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
});