/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import { createSelector } from '@ngrx/store';

import * as chainListPageActions from './chain-list-page.actions';
import { ChainModel } from './chain.model';

export interface ChainListPageState {
  loading: boolean;
  createModalVisible: boolean;
  error: string;
  items: ChainModel[];
}

export const initialState: ChainListPageState = {
  loading: false,
  createModalVisible: false,
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
        createModalVisible: false,
        items: action.chains
      };
    }
    case chainListPageActions.LOAD_CHAINS_FAIL: {
      return {
        ...state,
        createModalVisible: false,
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
    case chainListPageActions.SHOW_CREATE_MODAL: {
      return {
        ...state,
        createModalVisible: true,
      }
    }
    case chainListPageActions.HIDE_CREATE_MODAL: {
      return {
        ...state,
        createModalVisible: false,
      }
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

export const getLoading = createSelector(
    getChainListPageState,
    (state: ChainListPageState) => state.loading
);

export const getCreateModalVisible = createSelector(
    getChainListPageState,
    (state: ChainListPageState) => state.createModalVisible
);
