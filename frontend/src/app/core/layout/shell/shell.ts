import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from '../../notification/toast-container/toast-container';
import { Header } from '../header/header';
import { Menu } from '../menu/menu';

@Component({
  imports: [RouterOutlet, Header, Menu, ToastContainer],
  selector: 'app-shell',
  templateUrl: './shell.html',
})
export class Shell {
  protected readonly menuOpen = signal(false);
}