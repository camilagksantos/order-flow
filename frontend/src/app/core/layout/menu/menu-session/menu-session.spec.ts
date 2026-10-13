import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../auth/auth.store';
import { Toaster } from '../../../notification/toaster';
import { MenuSession } from './menu-session';

describe('MenuSession', () => {
  const isAuthenticated = signal(false);
  const logout = vi.fn();
  let component: MenuSession;
  let fixture: ComponentFixture<MenuSession>;
  let host: HTMLElement;
  let toaster: Toaster;
  let navigateSpy: MockInstance;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const signInLink = () => host.querySelector<HTMLAnchorElement>('a[href="/login"]');
  const signOutButton = () => host.querySelector<HTMLButtonElement>('button');

  beforeEach(async () => {
    isAuthenticated.set(false);
    logout.mockReset();
    logout.mockResolvedValue(undefined);

    await TestBed.configureTestingModule({
      imports: [MenuSession],
      providers: [provideRouter([]), { provide: AuthStore, useValue: { isAuthenticated, logout } }],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuSession);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    toaster = TestBed.inject(Toaster);
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should offer to sign in to a guest', () => {
    expect(signInLink()).not.toBeNull();
    expect(signOutButton()).toBeNull();
  });

  it('should offer to sign out to a logged-in user', async () => {
    isAuthenticated.set(true);
    await render();

    expect(signOutButton()).not.toBeNull();
    expect(signInLink()).toBeNull();
  });

  it('should emit chosen when the sign in link is clicked', () => {
    const chosen = vi.fn();
    component.chosen.subscribe(chosen);

    signInLink()!.click();

    expect(chosen).toHaveBeenCalledTimes(1);
  });

  it('should end the session, show a notice and go to the products page on sign out', async () => {
    const chosen = vi.fn();
    component.chosen.subscribe(chosen);
    isAuthenticated.set(true);
    await render();

    signOutButton()!.click();
    await fixture.whenStable();

    expect(logout).toHaveBeenCalledTimes(1);
    expect(toaster.toasts()[0].message).toBe('Sessão terminada');
    expect(navigateSpy).toHaveBeenCalledWith('/products');
    expect(chosen).toHaveBeenCalledTimes(1);
  });

  it('should end the session even when the backend call fails', async () => {
    logout.mockResolvedValue(undefined);
    isAuthenticated.set(true);
    await render();

    signOutButton()!.click();
    await fixture.whenStable();

    expect(toaster.toasts()).toHaveLength(1);
  });
});