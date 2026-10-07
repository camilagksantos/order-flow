import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest } from '../../models/auth/login-request';
import { TokenResponse } from '../../models/auth/token-response';

@Service()
export class AuthApi {
    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${environment.apiUrl}/api/v1/auth`;

    login(request: LoginRequest): Observable<TokenResponse> {
        return this.http.post<TokenResponse>(`${this.baseUrl}/login`, request, { withCredentials: true });
    }

    refresh(): Observable<TokenResponse> {
        return this.http.post<TokenResponse>(`${this.baseUrl}/refresh`, null, { withCredentials: true });
    }
}