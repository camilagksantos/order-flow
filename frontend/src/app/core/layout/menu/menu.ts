import { Component, ElementRef, effect, input, output, viewChild } from '@angular/core';
import { MenuLinks } from './menu-links/menu-links';
import { MenuSession } from './menu-session/menu-session';
import { MenuGreeting } from './menu-greeting/menu-greeting';

@Component({
  imports: [MenuGreeting, MenuLinks, MenuSession],
  selector: 'app-menu',
  templateUrl: './menu.html',
  host: { '(document:keydown.escape)': 'onEscape()' },
})
export class Menu {
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  readonly open = input(false);
  readonly closed = output<void>();

  constructor() {
    effect(() => {
      this.panel()?.nativeElement.querySelector<HTMLElement>('a, button')?.focus();
    });
  }

  protected onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }
}