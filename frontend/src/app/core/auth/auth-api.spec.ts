import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthApi } from './auth-api';

describe('AuthApi', () => {
    let api: AuthApi;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        api = TestBed.inject(AuthApi);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('should post the credentials to the login route with credentials enabled', () => {
        const response = { accessToken: 'token', tokenType: 'Bearer', expiresIn: 900 };
        let received: unknown;

        api.login({ email: 'ana@exemplo.pt', password: 'Password123' }).subscribe((value) => (received = value));

        const req = http.expectOne(`${environment.apiUrl}/api/v1/auth/login`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ email: 'ana@exemplo.pt', password: 'Password123' });
        expect(req.request.withCredentials).toBe(true);

        req.flush(response);
        expect(received).toEqual(response);
    });

    it('should post to the refresh route without a body and with credentials enabled', () => {
        const response = { accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 900 };
        let received: unknown;

        api.refresh().subscribe((value) => (received = value));

        const req = http.expectOne(`${environment.apiUrl}/api/v1/auth/refresh`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBeNull();
        expect(req.request.withCredentials).toBe(true);

        req.flush(response);
        expect(received).toEqual(response);
    });
});