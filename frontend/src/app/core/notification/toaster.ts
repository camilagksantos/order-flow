import { Service, signal } from '@angular/core';
import { Toast } from '../../models/toast/toast';
import { ToastKind } from '../../models/toast/toast-kind';

type Countdown = {
    remaining: number;
    startedAt: number;
    handle: ReturnType<typeof setTimeout> | null;
};

const maxVisible = 3;

const durations: Record<ToastKind, number> = {
    success: 4000,
    info: 4000,
    error: 7000,
};

@Service()
export class Toaster {
    private nextId = 1;
    private readonly toastList = signal<Toast[]>([]);
    private readonly countdowns = new Map<number, Countdown>();

    readonly toasts = this.toastList.asReadonly();

    success(message: string): void {
        this.show('success', message);
    }

    error(message: string): void {
        this.show('error', message);
    }

    info(message: string): void {
        this.show('info', message);
    }

    dismiss(id: number): void {
        this.clearCountdown(id);
        this.toastList.update((list) => list.filter((toast) => toast.id !== id));
    }

    pause(id: number): void {
        const countdown = this.countdowns.get(id);
        if (!countdown || countdown.handle === null) {
            return;
        }
        clearTimeout(countdown.handle);
        countdown.remaining -= Date.now() - countdown.startedAt;
        countdown.handle = null;
    }

    resume(id: number): void {
        const countdown = this.countdowns.get(id);
        if (!countdown || countdown.handle !== null) {
            return;
        }
        this.start(id, Math.max(countdown.remaining, 0));
    }

    private show(kind: ToastKind, message: string): void {
        const toast: Toast = { id: this.nextId++, kind, message, duration: durations[kind] };
        const all = [toast, ...this.toastList()];
        all.slice(maxVisible).forEach((dropped) => this.clearCountdown(dropped.id));
        this.toastList.set(all.slice(0, maxVisible));
        this.start(toast.id, toast.duration);
    }

    private start(id: number, remaining: number): void {
        const handle = setTimeout(() => this.dismiss(id), remaining);
        this.countdowns.set(id, { remaining, startedAt: Date.now(), handle });
    }

    private clearCountdown(id: number): void {
        const countdown = this.countdowns.get(id);
        if (countdown?.handle) {
            clearTimeout(countdown.handle);
        }
        this.countdowns.delete(id);
    }
}