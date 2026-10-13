import { Component, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-header-search-button',
  templateUrl: './header-search-button.html',
})
export class HeaderSearchButton {
  readonly open = input(false);
  readonly toggled = output<void>();
}
