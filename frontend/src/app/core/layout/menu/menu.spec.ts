import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../auth/auth.store';
import { Menu } from './menu';

describe('Menu', () => {
  let component: Menu;
  let fixture: ComponentFixture<Menu>;
  let host: HTMLElement;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const open = async () => {
    fixture.componentRef.setInput('open', true);
    await render();
  };

  const panel = () => host.querySelector('#main-menu');
  const backdrop = () => host.querySelector<HTMLElement>('div[aria-hidden="true"]');
  const productsLink = () => host.querySelector<HTMLAnchorElement>('a[href="/products"]');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Menu],
      providers: [
        provideRouter([{ path: 'products', children: [] }]),
        { provide: AuthStore, useValue: { isAuthenticated: signal(false) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Menu);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render no panel while closed', () => {
    expect(panel()).toBeNull();
  });

  it('should render the panel with the page links when open', async () => {
    await open();

    expect(panel()).not.toBeNull();
    expect(productsLink()).not.toBeNull();
  });

  it('should emit closed when the backdrop is clicked', async () => {
    const closed = vi.fn();
    component.closed.subscribe(closed);
    await open();

    backdrop()!.click();

    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('should emit closed on Escape while open', async () => {
    const closed = vi.fn();
    component.closed.subscribe(closed);
    await open();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('should ignore Escape while closed', () => {
    const closed = vi.fn();
    component.closed.subscribe(closed);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(closed).not.toHaveBeenCalled();
  });

  it('should emit closed when a page is chosen', async () => {
    const closed = vi.fn();
    component.closed.subscribe(closed);
    await open();

    productsLink()!.click();

    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('should focus the first link when it opens', async () => {
    await open();

    expect(document.activeElement).toBe(productsLink());
  });
});