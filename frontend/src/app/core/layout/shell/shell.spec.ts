import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../auth/auth.store';
import { CustomerStore } from '../../customer/customer.store';
import { Shell } from './shell';

describe('Shell', () => {
  let component: Shell;
  let fixture: ComponentFixture<Shell>;
  let host: HTMLElement;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const menuButton = () => host.querySelector<HTMLButtonElement>('button[aria-controls="main-menu"]')!;
  const backdrop = () => host.querySelector<HTMLElement>('div[aria-hidden="true"]')!;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: { isAuthenticated: signal(false), customerId: signal(null) } },
        { provide: CustomerStore, useValue: { status: signal('idle'), firstName: signal(null) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the toolbar, the page area and the toast container', () => {
    expect(host.querySelector('app-header')).not.toBeNull();
    expect(host.querySelector('main')).not.toBeNull();
    expect(host.querySelector('app-toast-container')).not.toBeNull();
  });

  it('should open the menu when the menu button is clicked', async () => {
    menuButton().click();
    await render();

    expect(host.querySelector('#main-menu')).not.toBeNull();
    expect(menuButton().getAttribute('aria-label')).toBe('Fechar menu');
  });

  it('should close the menu when the backdrop is clicked', async () => {
    menuButton().click();
    await render();

    backdrop().click();
    await render();

    expect(menuButton().getAttribute('aria-label')).toBe('Abrir menu');
  });

  it('should close the menu on Escape', async () => {
    menuButton().click();
    await render();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await render();

    expect(menuButton().getAttribute('aria-label')).toBe('Abrir menu');
  });
});