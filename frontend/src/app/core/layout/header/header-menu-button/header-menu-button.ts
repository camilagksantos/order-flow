import { Component, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-header-menu-button',
  templateUrl: './header-menu-button.html',
})
export class HeaderMenuButton {
  readonly open = input(false);
  readonly toggled = output<void>();
}
