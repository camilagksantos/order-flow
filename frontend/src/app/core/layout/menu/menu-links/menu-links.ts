import { Component, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthStore } from '../../../auth/auth.store';

@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-menu-links',
  templateUrl: './menu-links.html',
})
export class MenuLinks {
  protected readonly auth = inject(AuthStore);

  readonly navigated = output<void>();
}
