import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { HeaderSearchButton } from './header-search-button';

describe('HeaderSearchButton', () => {
  let component: HeaderSearchButton;
  let fixture: ComponentFixture<HeaderSearchButton>;

  const button = () => fixture.nativeElement.querySelector('button') as HTMLButtonElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderSearchButton],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderSearchButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should offer to search while the panel is closed', () => {
    expect(button().getAttribute('aria-label')).toBe('Pesquisar');
    expect(button().getAttribute('aria-expanded')).toBe('false');
  });

  it('should offer to close the search while the panel is open', async () => {
    fixture.componentRef.setInput('open', true);
    await fixture.whenStable();

    expect(button().getAttribute('aria-label')).toBe('Fechar pesquisa');
    expect(button().getAttribute('aria-expanded')).toBe('true');
  });

  it('should emit toggled when clicked', () => {
    const toggled = vi.fn();
    component.toggled.subscribe(toggled);

    button().click();

    expect(toggled).toHaveBeenCalledTimes(1);
  });
});