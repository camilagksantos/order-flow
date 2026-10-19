import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, TitleStrategy, withInMemoryScrolling } from '@angular/router';
import { PageTitleStrategy } from './core/layout/page-title-strategy';
import { routes } from './app.routes';
import { AuthStore } from './core/auth/auth.store';
import { CustomerStore } from './core/customer/customer.store';
import { authInterceptor } from './core/interceptor/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    { provide: TitleStrategy, useClass: PageTitleStrategy },
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => inject(AuthStore).restoreSession()),
    provideAppInitializer(() => {
      inject(CustomerStore);
    })
  ]
};