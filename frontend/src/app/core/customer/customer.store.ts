import { computed, effect, inject, untracked } from '@angular/core';
import {
  patchState,
  signalStore,
  withComputed,
  withHooks,
  withMethods,
  withState
} from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { Customer } from '../../models/customer/customer';
import { LoadStatus } from '../../models/shared/load-status';
import { AuthStore } from '../auth/auth.store';
import { CustomerApi } from './customer-api';

type CustomerState = {
  customer: Customer | null;
  status: LoadStatus;
};

const initialCustomerState: CustomerState = {
  customer: null,
  status: 'idle'
};

export const CustomerStore = signalStore(
  { providedIn: 'root' },
  withState(initialCustomerState),
  withComputed((store) => ({
    firstName: computed(() => store.customer()?.name.trim().split(/\s+/)[0] || null)
  })),
  withMethods((store, api = inject(CustomerApi), auth = inject(AuthStore)) => ({
    async load(id: number): Promise<void> {
      patchState(store, { customer: null, status: 'loading' });
      try {
        const customer = await firstValueFrom(api.findById(id));
        if (auth.customerId() !== id) {
          return;
        }
        patchState(store, { customer, status: 'loaded' });
      } catch {
        if (auth.customerId() !== id) {
          return;
        }
        patchState(store, { customer: null, status: 'error' });
      }
    },
    clear(): void {
      patchState(store, initialCustomerState);
    }
  })),
  withHooks((store, auth = inject(AuthStore)) => ({
    onInit() {
      effect(() => {
        const id = auth.customerId();
        untracked(() => {
          if (id === null) {
            store.clear();
          } else {
            void store.load(id);
          }
        });
      });
    }
  }))
);