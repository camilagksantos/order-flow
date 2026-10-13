import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { Header } from './header';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let host: HTMLElement;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const menuButton = () => host.querySelector<HTMLButtonElement>('button[aria-controls="main-menu"]')!;
  const searchToggle = () =>
    host.querySelector<HTMLButtonElement>('button[aria-label="Pesquisar"], button[aria-label="Fechar pesquisa"]')!;
  const searchInput = () => host.querySelector<HTMLInputElement>('input[aria-label="Pesquisar produtos"]');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the brand name linked to the products page', () => {
    const brand = host.querySelector<HTMLAnchorElement>('a[href="/products"]')!;

    expect(brand.textContent).toContain('Velora');
  });

  it('should link the cart icon to the cart', () => {
    expect(host.querySelector('a[href="/cart"]')).not.toBeNull();
  });

  it('should emit menuToggle when the menu button is clicked', () => {
    const toggled = vi.fn();
    component.menuToggle.subscribe(toggled);

    menuButton().click();

    expect(toggled).toHaveBeenCalledTimes(1);
  });

  it('should label the menu button according to the menuOpen input', async () => {
    expect(menuButton().getAttribute('aria-label')).toBe('Abrir menu');

    fixture.componentRef.setInput('menuOpen', true);
    await render();

    expect(menuButton().getAttribute('aria-label')).toBe('Fechar menu');
  });

  it('should open the search panel when the search button is clicked', async () => {
    expect(searchInput()).toBeNull();

    searchToggle().click();
    await render();

    expect(searchInput()).not.toBeNull();
    expect(searchToggle().getAttribute('aria-label')).toBe('Fechar pesquisa');
  });

  it('should close the search panel when the search button is clicked again', async () => {
    searchToggle().click();
    await render();

    searchToggle().click();
    await render();

    expect(searchToggle().getAttribute('aria-label')).toBe('Pesquisar');
  });

  it('should close the search panel when the panel asks to close', async () => {
    searchToggle().click();
    await render();

    searchInput()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await render();

    expect(searchToggle().getAttribute('aria-label')).toBe('Pesquisar');
  });
});