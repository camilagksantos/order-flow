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
import { AccessTokenPayload } from '../../models/auth/access-token-payload';
import { AuthUser } from '../../models/auth/auth-user';
import { LoginRequest } from '../../models/auth/login-request';
import { AuthApi } from './auth-api';

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
  withMethods((store, authApi = inject(AuthApi)) => {
    let refreshInFlight: Promise<void> | null = null;

    const startSession = (accessToken: string): void => {
      const payload = jwtDecode<AccessTokenPayload>(accessToken);
      patchState(store, {
        accessToken,
        user: {
          email: payload.sub,
          roles: payload.roles,
          customerId: payload.customerId ?? null
        }
      });
    };

    const clearSession = (): void => {
      patchState(store, initialAuthState);
    };

    const runRefresh = async (): Promise<void> => {
      try {
        const response = await firstValueFrom(authApi.refresh());
        startSession(response.accessToken);
      } catch (error) {
        clearSession();
        throw error;
      }
    };

    const refresh = (): Promise<void> => {
      if (!refreshInFlight) {
        refreshInFlight = runRefresh().finally(() => {
          refreshInFlight = null;
        });
      }
      return refreshInFlight;
    };

    return {
      async login(request: LoginRequest): Promise<void> {
        const response = await firstValueFrom(authApi.login(request));
        startSession(response.accessToken);
      },
      async logout(): Promise<void> {
        await firstValueFrom(authApi.logout()).catch(() => undefined);
        clearSession();
      },
      async restoreSession(): Promise<void> {
        await refresh().catch(() => undefined);
      },
      refresh,
      clearSession
    };
  })
);