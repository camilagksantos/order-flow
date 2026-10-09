import { Component, inject } from '@angular/core';
import { Toaster } from '../toaster';
import { ToastKind } from '../../../models/toast/toast-kind';

@Component({
  imports: [],
  selector: 'app-toast-container',
  templateUrl: './toast-container.html',
})
export class ToastContainer {
  protected readonly toaster = inject(Toaster);

  protected readonly accentClass: Record<ToastKind, string> = {
    success: 'border-l-success',
    error: 'border-l-danger',
    info: 'border-l-wine-600',
  };

  protected readonly iconClass: Record<ToastKind, string> = {
    success: 'text-success',
    error: 'text-danger',
    info: 'text-wine-600',
  };
}
