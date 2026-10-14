import { Component, computed, inject } from '@angular/core';
import { AuthStore } from '../../../auth/auth.store';
import { CustomerStore } from '../../../customer/customer.store';

type GreetingView = 'visitor' | 'loading' | 'named' | 'generic';

@Component({
  imports: [],
  selector: 'app-menu-greeting',
  templateUrl: './menu-greeting.html',
})
export class MenuGreeting {
  private readonly auth = inject(AuthStore);
  private readonly customerStore = inject(CustomerStore);

  protected readonly firstName = this.customerStore.firstName;

  protected readonly view = computed<GreetingView>(() => {
    if (!this.auth.isAuthenticated()) {
      return 'visitor';
    }
    const status = this.customerStore.status();
    if (status === 'loaded') {
      return this.customerStore.firstName() ? 'named' : 'generic';
    }
    if (status === 'error' || this.auth.customerId() === null) {
      return 'generic';
    }
    return 'loading';
  });
}