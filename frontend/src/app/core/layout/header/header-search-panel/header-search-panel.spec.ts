import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MockInstance, vi } from 'vitest';
import { SearchStore } from '../../../search/search.store';
import { HeaderSearchPanel } from './header-search-panel';

describe('SearchPanel', () => {
  let component: HeaderSearchPanel;
  let fixture: ComponentFixture<HeaderSearchPanel>;
  let host: HTMLElement;
  let searchStore: InstanceType<typeof SearchStore>;
  let navigateSpy: MockInstance;

  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const input = () => host.querySelector<HTMLInputElement>('input[aria-label="Pesquisar produtos"]')!;
  const suggestion = () =>
    Array.from(host.querySelectorAll<HTMLButtonElement>('button')).find((button) =>
      button.textContent?.includes('Pesquisar «'),
    );

  const type = async (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    await render();
  };

  const press = async (key: string) => {
    input().dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
    await render();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderSearchPanel],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderSearchPanel);
    component = fixture.componentInstance;
    host = fixture.nativeElement;
    searchStore = TestBed.inject(SearchStore);
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should focus the field when it opens', () => {
    expect(document.activeElement).toBe(input());
  });

  it('should show the search line while typing without applying the term', async () => {
    await type('vela');

    expect(suggestion()).toBeDefined();
    expect(suggestion()!.textContent).toContain('vela');
    expect(searchStore.term()).toBe('');
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('should not show the search line for blank text', async () => {
    await type('   ');

    expect(suggestion()).toBeUndefined();
  });

  it('should apply the term and go to the products page on Enter', async () => {
    await type('vela');

    await press('Enter');

    expect(searchStore.term()).toBe('vela');
    expect(navigateSpy).toHaveBeenCalledWith('/products');
  });

  it('should apply the term and go to the products page when the line is clicked', async () => {
    await type('vela');

    suggestion()!.click();

    expect(searchStore.term()).toBe('vela');
    expect(navigateSpy).toHaveBeenCalledWith('/products');
  });

  it('should clear the search and ask to close on Escape', async () => {
    const closed = vi.fn();
    component.closed.subscribe(closed);
    await type('vela');
    await press('Enter');

    await press('Escape');

    expect(searchStore.term()).toBe('');
    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('should clear the search and ask to close with the close button', async () => {
    const closed = vi.fn();
    component.closed.subscribe(closed);
    await type('vela');
    await press('Enter');

    host.querySelector<HTMLButtonElement>('button[aria-label="Limpar e fechar pesquisa"]')!.click();

    expect(searchStore.term()).toBe('');
    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('should clear the search when it is destroyed', () => {
    searchStore.setTerm('vela');

    fixture.destroy();

    expect(searchStore.term()).toBe('');
  });
});