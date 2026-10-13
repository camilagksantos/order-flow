import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { HeaderMenuButton } from './header-menu-button';

describe('HeaderMenuButton', () => {
  let component: HeaderMenuButton;
  let fixture: ComponentFixture<HeaderMenuButton>;

  const button = () => fixture.nativeElement.querySelector('button') as HTMLButtonElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderMenuButton],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderMenuButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should offer to open the menu while it is closed', () => {
    expect(button().getAttribute('aria-label')).toBe('Abrir menu');
    expect(button().getAttribute('aria-expanded')).toBe('false');
  });

  it('should offer to close the menu while it is open', async () => {
    fixture.componentRef.setInput('open', true);
    await fixture.whenStable();

    expect(button().getAttribute('aria-label')).toBe('Fechar menu');
    expect(button().getAttribute('aria-expanded')).toBe('true');
  });

  it('should emit toggled when clicked', () => {
    const toggled = vi.fn();
    component.toggled.subscribe(toggled);

    button().click();

    expect(toggled).toHaveBeenCalledTimes(1);
  });
});