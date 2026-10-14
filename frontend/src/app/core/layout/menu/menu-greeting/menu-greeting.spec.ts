import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoadStatus } from '../../../../models/shared/load-status';
import { AuthStore } from '../../../auth/auth.store';
import { CustomerStore } from '../../../customer/customer.store';
import { MenuGreeting } from './menu-greeting';

describe('MenuGreeting', () => {
  const isAuthenticated = signal(false);
  const customerId = signal<number | null>(null);
  const status = signal<LoadStatus>('idle');
  const firstName = signal<string | null>(null);
  let component: MenuGreeting;
  let fixture: ComponentFixture<MenuGreeting>;
  let host: HTMLElement;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    isAuthenticated.set(false);
    customerId.set(null);
    status.set('idle');
    firstName.set(null);

    await TestBed.configureTestingModule({
      imports: [MenuGreeting],
      providers: [
        { provide: AuthStore, useValue: { isAuthenticated, customerId } },
        { provide: CustomerStore, useValue: { status, firstName } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuGreeting);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should greet a guest', () => {
    expect(host.textContent).toContain('Olá, visitante');
  });

  it('should show a placeholder while the name is loading', async () => {
    isAuthenticated.set(true);
    customerId.set(7);
    status.set('loading');
    await render();

    expect(host.querySelector('[role="status"]')).not.toBeNull();
    expect(host.textContent).not.toContain('Olá');
  });

  it('should greet the customer by the first name', async () => {
    isAuthenticated.set(true);
    customerId.set(7);
    status.set('loaded');
    firstName.set('Ana');
    await render();

    expect(host.textContent).toContain('Olá, Ana');
  });

  it('should fall back to a generic greeting when loading the customer fails', async () => {
    isAuthenticated.set(true);
    customerId.set(7);
    status.set('error');
    await render();

    expect(host.textContent).toContain('Olá!');
  });

  it('should use a generic greeting for an account without a customer record', async () => {
    isAuthenticated.set(true);
    customerId.set(null);
    status.set('idle');
    await render();

    expect(host.textContent).toContain('Olá!');
  });
});