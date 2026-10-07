import { computed, inject } from '@angular/core';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState
} from '@ngrx/signals';
import { jwtDecode } from 'jwt-decode';
import { firstValueFrom } from 'rxjs';

import { AuthApi } from './auth-api';
import { AccessTokenPayload } from '../../models/auth/access-token-payload';
import { AuthUser } from '../../models/auth/auth-user';
import { LoginRequest } from '../../models/auth/login-request';

type AuthState = {
  accessToken: string | null;
  user: AuthUser | null;
};

const initialAuthState: AuthState = {
  accessToken: null,
  user: null
};

export const AuthStore = signalStore(
  { providedIn: 'root' },
  withState(initialAuthState),
  withComputed((store) => ({
    isAuthenticated: computed(() => store.accessToken() !== null),
    isAdmin: computed(() => store.user()?.roles.includes('ROLE_ADMIN') ?? false),
    customerId: computed(() => store.user()?.customerId ?? null)
  })),
  withMethods((store, authApi = inject(AuthApi)) => ({
    async login(request: LoginRequest): Promise<void> {
      const response = await firstValueFrom(authApi.login(request));
      const payload = jwtDecode<AccessTokenPayload>(response.accessToken);
      patchState(store, {
        accessToken: response.accessToken,
        user: {
          email: payload.sub,
          roles: payload.roles,
          customerId: payload.customerId ?? null
        }
      });
    }
  }))
);