import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Toaster } from './toaster';

describe('Toaster', () => {
    let toaster: Toaster;

    beforeEach(() => {
        vi.useFakeTimers();
        toaster = TestBed.inject(Toaster);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should start without toasts', () => {
        expect(toaster.toasts()).toEqual([]);
    });

    it('should add a toast with the kind, the message and the duration of its kind', () => {
        toaster.error('Palavra-passe incorreta');

        expect(toaster.toasts()).toEqual([
            { id: expect.any(Number), kind: 'error', message: 'Palavra-passe incorreta', duration: 7000 },
        ]);
    });

    it('should put the newest toast first', () => {
        toaster.info('primeiro');
        toaster.success('segundo');

        expect(toaster.toasts().map((toast) => toast.message)).toEqual(['segundo', 'primeiro']);
    });

    it('should keep at most three toasts and drop the oldest', () => {
        ['um', 'dois', 'três', 'quatro'].forEach((message) => toaster.info(message));

        expect(toaster.toasts().map((toast) => toast.message)).toEqual(['quatro', 'três', 'dois']);
    });

    it('should remove a dismissed toast', () => {
        toaster.info('um');
        const [toast] = toaster.toasts();

        toaster.dismiss(toast.id);

        expect(toaster.toasts()).toEqual([]);
    });

    it('should remove a success toast after four seconds', () => {
        toaster.success('feito');

        vi.advanceTimersByTime(3999);
        expect(toaster.toasts()).toHaveLength(1);

        vi.advanceTimersByTime(1);
        expect(toaster.toasts()).toHaveLength(0);
    });

    it('should keep an error toast for seven seconds', () => {
        toaster.error('falhou');

        vi.advanceTimersByTime(4000);
        expect(toaster.toasts()).toHaveLength(1);

        vi.advanceTimersByTime(3000);
        expect(toaster.toasts()).toHaveLength(0);
    });

    it('should stop the countdown while paused and continue with the remaining time', () => {
        toaster.error('falhou');
        const [toast] = toaster.toasts();

        vi.advanceTimersByTime(3000);
        toaster.pause(toast.id);
        vi.advanceTimersByTime(20000);
        expect(toaster.toasts()).toHaveLength(1);

        toaster.resume(toast.id);
        vi.advanceTimersByTime(3999);
        expect(toaster.toasts()).toHaveLength(1);

        vi.advanceTimersByTime(1);
        expect(toaster.toasts()).toHaveLength(0);
    });
});