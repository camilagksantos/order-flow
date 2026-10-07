import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth-interceptor';
import { AuthStore } from './auth.store';

const unauthorized = { status: 401, statusText: 'Unauthorized' };
const flushPromises = () => new Promise<void>((resolve) => setTimeout(resolve));

describe('authInterceptor', () => {
  const accessToken = signal<string | null>(null);
  const store = { accessToken, refresh: vi.fn() };
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    accessToken.set(null);
    store.refresh.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthStore, useValue: store },
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

  it('should refresh the token and repeat the call once after a 401', async () => {
    accessToken.set('old-token');
    store.refresh.mockImplementation(async () => {
      accessToken.set('new-token');
    });
    const url = `${environment.apiUrl}/api/v1/orders/1`;
    let result: unknown;

    client.get(url).subscribe((value) => (result = value));
    http.expectOne(url).flush(null, unauthorized);
    await flushPromises();

    const retry = http.expectOne(url);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new-token');
    retry.flush({ id: 1 });

    expect(result).toEqual({ id: 1 });
    expect(store.refresh).toHaveBeenCalledTimes(1);
  });

  it('should return the error when the refresh fails', async () => {
    accessToken.set('old-token');
    store.refresh.mockRejectedValue(new Error('refresh failed'));
    const url = `${environment.apiUrl}/api/v1/orders/1`;
    let error: unknown;

    client.get(url).subscribe({ error: (e) => (error = e) });
    http.expectOne(url).flush(null, unauthorized);
    await flushPromises();

    expect((error as Error).message).toBe('refresh failed');
  });

  it('should return the error when the repeated call is also a 401', async () => {
    accessToken.set('old-token');
    store.refresh.mockImplementation(async () => {
      accessToken.set('new-token');
    });
    const url = `${environment.apiUrl}/api/v1/orders/1`;
    let error: unknown;

    client.get(url).subscribe({ error: (e) => (error = e) });
    http.expectOne(url).flush(null, unauthorized);
    await flushPromises();
    http.expectOne(url).flush(null, unauthorized);

    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect(store.refresh).toHaveBeenCalledTimes(1);
  });

  it('should not refresh on errors other than 401', () => {
    accessToken.set('token-123');
    const url = `${environment.apiUrl}/api/v1/orders/1`;
    let error: HttpErrorResponse | undefined;

    client.get(url).subscribe({ error: (e) => (error = e) });
    http.expectOne(url).flush(null, { status: 500, statusText: 'Server Error' });

    expect(error?.status).toBe(500);
    expect(store.refresh).not.toHaveBeenCalled();
  });

  it('should not refresh on a 401 from an auth route', () => {
    accessToken.set('token-123');
    const url = `${environment.apiUrl}/api/v1/auth/login`;
    let error: HttpErrorResponse | undefined;

    client.post(url, {}).subscribe({ error: (e) => (error = e) });
    http.expectOne(url).flush(null, unauthorized);

    expect(error?.status).toBe(401);
    expect(store.refresh).not.toHaveBeenCalled();
  });

  it('should not refresh on a 401 when nobody is logged in', () => {
    const url = `${environment.apiUrl}/api/v1/orders/1`;
    let error: HttpErrorResponse | undefined;

    client.get(url).subscribe({ error: (e) => (error = e) });
    http.expectOne(url).flush(null, unauthorized);

    expect(error?.status).toBe(401);
    expect(store.refresh).not.toHaveBeenCalled();
  });
});