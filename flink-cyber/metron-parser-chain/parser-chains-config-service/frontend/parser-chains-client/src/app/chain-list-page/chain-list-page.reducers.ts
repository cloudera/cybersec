import { createSelector } from '@ngrx/store';

import * as chainListPageActions from './chain-list-page.actions';
import { ChainModel } from './chain.model';

export interface ChainListPageState {
  loading: boolean;
  error: string;
  items: ChainModel[];
}

export const initialState: ChainListPageState = {
  loading: false,
  items: [],
  error: ''
};

export function reducer(
  state: ChainListPageState = initialState,
  action: chainListPageActions.ChainListAction
): ChainListPageState {
  switch (action.type) {
    case chainListPageActions.LOAD_CHAINS: {
      return {
        ...state,
        loading: true,
      };
    }
    case chainListPageActions.LOAD_CHAINS_SUCCESS: {
      return {
        ...state,
        loading: false,
        items: action.chains
      };
    }
    case chainListPageActions.LOAD_CHAINS_FAIL: {
      return {
        ...state,
        error: action.error.message,
        loading: false,
      };
    }
    case chainListPageActions.CREATE_CHAIN: {
      return {
        ...state,
        loading: true,
      };
    }
    case chainListPageActions.CREATE_CHAIN_SUCCESS: {
      return {
        ...state,
        loading: false,
        items: [...state.items, action.chain]
      };
    }
    case chainListPageActions.CREATE_CHAIN_FAIL: {
      return {
        ...state,
        error: action.error.message,
        loading: false,
      };
    }
    case chainListPageActions.DELETE_CHAIN: {
      return {
        ...state,
        loading: true,
      };
    }
    case chainListPageActions.DELETE_CHAIN_SUCCESS: {
      return {
        ...state,
        loading: false,
        items: state.items.filter(chainItem => chainItem.id !== action.chainId)
      };
    }
    case chainListPageActions.DELETE_CHAIN_FAIL: {
      return {
        ...state,
        error: action.error.message,
        loading: false,
      };
    }
    default: {
      return state;
    }
  }
}

export function getChainListPageState(state: any): ChainListPageState {
  return state['chain-list-page'];
}

export const getChains = createSelector(
  getChainListPageState,
  (state: ChainListPageState) => state.items
);
