import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from './core/notification/toast-container/toast-container';

@Component({
  imports: [RouterOutlet, ToastContainer],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
}
