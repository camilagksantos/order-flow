import { Component, DestroyRef, ElementRef, computed, effect, inject, output, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { SearchStore } from '../../../search/search.store';

@Component({
  imports: [],
  selector: 'app-header-search-panel',
  templateUrl: './header-search-panel.html',
})
export class HeaderSearchPanel {
  private readonly router = inject(Router);
  private readonly searchStore = inject(SearchStore);
  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  readonly closed = output<void>();

  protected readonly draft = signal('');
  protected readonly trimmedDraft = computed(() => this.draft().trim());

  constructor() {
    effect(() => {
      this.searchInput()?.nativeElement.focus();
    });
    inject(DestroyRef).onDestroy(() => this.searchStore.clear());
  }

  protected onInput(event: Event): void {
    this.draft.set((event.target as HTMLInputElement).value);
  }

  protected submit(): void {
    const term = this.trimmedDraft();
    if (!term) {
      return;
    }
    this.searchStore.setTerm(term);
    void this.router.navigateByUrl('/products');
  }

  protected close(): void {
    this.searchStore.clear();
    this.draft.set('');
    this.closed.emit();
  }
}