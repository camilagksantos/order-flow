import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../../auth/auth.store';
import { MenuLinks } from './menu-links';

describe('MenuLinks', () => {
  const isAuthenticated = signal(false);
  let component: MenuLinks;
  let fixture: ComponentFixture<MenuLinks>;
  let host: HTMLElement;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const link = (href: string) => host.querySelector<HTMLAnchorElement>(`a[href="${href}"]`);

  beforeEach(async () => {
    isAuthenticated.set(false);
    await TestBed.configureTestingModule({
      imports: [MenuLinks],
      providers: [
        provideRouter([
          { path: 'products', children: [] },
          { path: 'orders', children: [] },
        ]),
        { provide: AuthStore, useValue: { isAuthenticated } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuLinks);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show only the products link to a guest', () => {
    expect(link('/products')).not.toBeNull();
    expect(link('/orders')).toBeNull();
  });

  it('should also show the orders link to a logged-in user', async () => {
    isAuthenticated.set(true);
    await render();

    expect(link('/orders')).not.toBeNull();
  });

  it('should emit navigated when a link is clicked', () => {
    const navigated = vi.fn();
    component.navigated.subscribe(navigated);

    link('/products')!.click();

    expect(navigated).toHaveBeenCalledTimes(1);
  });

  it('should mark the current page', async () => {
    await TestBed.inject(Router).navigateByUrl('/products');
    await render();

    expect(link('/products')!.getAttribute('aria-current')).toBe('page');
  });
});