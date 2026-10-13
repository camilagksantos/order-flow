import { computed } from '@angular/core';
import {
    patchState,
    signalStore,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

type SearchState = {
    term: string;
};

const initialSearchState: SearchState = {
    term: ''
};

export const SearchStore = signalStore(
    { providedIn: 'root' },
    withState(initialSearchState),
    withComputed((store) => ({
        hasTerm: computed(() => store.term().trim().length > 0)
    })),
    withMethods((store) => ({
        setTerm(term: string): void {
            patchState(store, { term });
        },
        clear(): void {
            patchState(store, initialSearchState);
        }
    }))
);