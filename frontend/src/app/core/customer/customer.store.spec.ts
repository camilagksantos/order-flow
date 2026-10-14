import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Customer } from '../../models/customer/customer';
import { AuthStore } from '../auth/auth.store';
import { CustomerApi } from './customer-api';
import { CustomerStore } from './customer.store';

describe('CustomerStore', () => {
    const customerId = signal<number | null>(null);
    const api = { findById: vi.fn() };
    let store: InstanceType<typeof CustomerStore>;

    const buildCustomer = (name: string): Customer => ({
        id: 7,
        name,
        email: 'ana@exemplo.pt',
        nif: '123456789',
        phone: null,
        status: 'ACTIVE',
        addresses: [],
    });

    beforeEach(() => {
        customerId.set(null);
        api.findById.mockReset();
        TestBed.configureTestingModule({
            providers: [
                { provide: CustomerApi, useValue: api },
                { provide: AuthStore, useValue: { customerId } },
            ],
        });
        store = TestBed.inject(CustomerStore);
        TestBed.tick();
    });

    it('should start idle without a customer', () => {
        expect(store.status()).toBe('idle');
        expect(store.customer()).toBeNull();
        expect(store.firstName()).toBeNull();
    });

    it('should load the customer when the session gets a customer id', async () => {
        const customer = buildCustomer('Ana Costa');
        api.findById.mockReturnValue(of(customer));

        customerId.set(7);
        TestBed.tick();

        await vi.waitFor(() => expect(store.status()).toBe('loaded'));
        expect(api.findById).toHaveBeenCalledWith(7);
        expect(store.customer()).toEqual(customer);
        expect(store.firstName()).toBe('Ana');
    });

    it('should be loading while the response has not arrived', async () => {
        const pending = new Subject<Customer>();
        api.findById.mockReturnValue(pending);

        customerId.set(7);
        TestBed.tick();

        expect(store.status()).toBe('loading');
        expect(store.firstName()).toBeNull();

        pending.next(buildCustomer('Ana Costa'));
        pending.complete();
        await vi.waitFor(() => expect(store.status()).toBe('loaded'));
    });

    it('should be in the error state when the request fails', async () => {
        api.findById.mockReturnValue(throwError(() => new Error('500')));

        customerId.set(7);
        TestBed.tick();

        await vi.waitFor(() => expect(store.status()).toBe('error'));
        expect(store.customer()).toBeNull();
        expect(store.firstName()).toBeNull();
    });

    it('should clear the customer when the session ends', async () => {
        api.findById.mockReturnValue(of(buildCustomer('Ana Costa')));
        customerId.set(7);
        TestBed.tick();
        await vi.waitFor(() => expect(store.status()).toBe('loaded'));

        customerId.set(null);
        TestBed.tick();

        expect(store.status()).toBe('idle');
        expect(store.customer()).toBeNull();
    });

    it('should ignore a late response after the session ended', async () => {
        const pending = new Subject<Customer>();
        api.findById.mockReturnValue(pending);
        customerId.set(7);
        TestBed.tick();
        customerId.set(null);
        TestBed.tick();

        pending.next(buildCustomer('Ana Costa'));
        pending.complete();
        await Promise.resolve();
        await Promise.resolve();

        expect(store.status()).toBe('idle');
        expect(store.customer()).toBeNull();
    });

    it('should use only the first word of the name as the first name', async () => {
        api.findById.mockReturnValue(of(buildCustomer('  Ana Maria Costa ')));

        customerId.set(7);
        TestBed.tick();

        await vi.waitFor(() => expect(store.firstName()).toBe('Ana'));
    });
});