import { Component, input, output, signal } from '@angular/core';
import { HeaderBrand } from './header-brand/header-brand';
import { HeaderCart } from './header-cart/header-cart';
import { HeaderMenuButton } from './header-menu-button/header-menu-button';
import { HeaderSearchButton } from './header-search-button/header-search-button';
import { HeaderSearchPanel } from './header-search-panel/header-search-panel';

@Component({
  imports: [HeaderBrand, HeaderCart, HeaderMenuButton, HeaderSearchButton, HeaderSearchPanel],
  selector: 'app-header',
  templateUrl: './header.html',
})
export class Header {
  readonly menuOpen = input(false);
  readonly menuToggle = output<void>();

  protected readonly searchOpen = signal(false);

  protected toggleSearch(): void {
    this.searchOpen.update((open) => !open);
  }
}