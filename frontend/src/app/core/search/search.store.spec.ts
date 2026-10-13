import { TestBed } from '@angular/core/testing';
import { SearchStore } from './search.store';

describe('SearchStore', () => {
    let store: InstanceType<typeof SearchStore>;

    beforeEach(() => {
        store = TestBed.inject(SearchStore);
    });

    it('should start without a term', () => {
        expect(store.term()).toBe('');
        expect(store.hasTerm()).toBe(false);
    });

    it('should keep the typed term', () => {
        store.setTerm('vela');

        expect(store.term()).toBe('vela');
        expect(store.hasTerm()).toBe(true);
    });

    it('should not count blank spaces as a term', () => {
        store.setTerm('   ');

        expect(store.hasTerm()).toBe(false);
    });

    it('should clear the term', () => {
        store.setTerm('vela');

        store.clear();

        expect(store.term()).toBe('');
        expect(store.hasTerm()).toBe(false);
    });
});