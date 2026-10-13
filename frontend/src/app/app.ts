import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from './core/notification/toast-container/toast-container';
import { Header } from './core/layout/header/header';
import { Menu } from './core/layout/menu/menu';

@Component({
  imports: [RouterOutlet, Header, Menu, ToastContainer],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  protected readonly menuOpen = signal(false);
}
