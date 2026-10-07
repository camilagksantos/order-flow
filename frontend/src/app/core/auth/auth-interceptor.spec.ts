import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth-interceptor';
import { AuthStore } from './auth.store';

describe('authInterceptor', () => {
  const accessToken = signal<string | null>(null);
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    accessToken.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthStore, useValue: { accessToken } },
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should add the bearer header to API calls when there is a token', () => {
    accessToken.set('token-123');
    const url = `${environment.apiUrl}/api/v1/orders/1`;

    client.get(url).subscribe();

    const req = http.expectOne(url);
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-123');
    req.flush({});
  });

  it('should not add the header when there is no token', () => {
    const url = `${environment.apiUrl}/api/v1/products`;

    client.get(url).subscribe();

    const req = http.expectOne(url);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should not add the header to the auth routes', () => {
    accessToken.set('token-123');
    const url = `${environment.apiUrl}/api/v1/auth/login`;

    client.post(url, {}).subscribe();

    const req = http.expectOne(url);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should not add the header to another domain', () => {
    accessToken.set('token-123');
    const url = 'https://outro-dominio.com/api/v1/orders';

    client.get(url).subscribe();

    const req = http.expectOne(url);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});