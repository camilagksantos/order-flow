import { Component, inject, output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Toaster } from '../../../notification/toaster';
import { AuthStore } from '../../../auth/auth.store';

@Component({
  imports: [RouterLink],
  selector: 'app-menu-session',
  templateUrl: './menu-session.html',
})
export class MenuSession {
  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);

  protected readonly auth = inject(AuthStore);

  readonly chosen = output<void>();

  protected async signOut(): Promise<void> {
    await this.auth.logout();
    this.toaster.info('Sessão terminada');
    await this.router.navigateByUrl('/products');
    this.chosen.emit();
  }
}
