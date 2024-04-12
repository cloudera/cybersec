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

import {createSelector} from '@ngrx/store';

import * as addParserActions from '../chain-add-parser-page/chain-add-parser-page.actions';
import {ChainModel} from '../chain-list-page/chain.model';

import * as chainPageActions from './chain-page.actions';
import {ParserChainModel, ParserModel, RouteModel} from './chain-page.models';
import {denormalizeParserConfig} from './chain-page.utils';
import {CustomFormConfig} from './components/custom-form/custom-form.component';
import * as chainListPageActions from "../chain-list-page/chain-list-page.actions";

export interface ChainPageState {
  chains: { [key: string]: ParserChainModel };
  parsers: { [key: string]: ParserModel };
  routes: { [key: string]: RouteModel };
  error: string;
  parserToBeInvestigated: string;
  failedParser: string;
  formConfigs?: { [key: string]: CustomFormConfig[] };
  dirtyParsers: string[];
  dirtyChains: string[];
  path: string[];
  indexMappings: { path: string, result: object };
}

export const initialState: ChainPageState = {
  chains: {},
  parsers: {},
  routes: {},
  error: '',
  parserToBeInvestigated: '',
  failedParser: '',
  formConfigs: {},
  dirtyParsers: [],
  dirtyChains: [],
  path: [],
  indexMappings: { path: '', result: {} },
};

export const uniqueAdd = (haystack: string[], needle: string): string[] => {
  return [ ...haystack.filter(item => item !== needle), needle];
};

export function reducer(
  state: ChainPageState = initialState,
  action: chainPageActions.ChainDetailsAction | addParserActions.ParserAction | chainListPageActions.ChainListAction | chainPageActions.IndexMappingAction
): ChainPageState {
  switch (action.type) {
    case chainListPageActions.PIPELINE_CHANGED: {
      return initialState
    }
    case chainPageActions.LOAD_CHAIN_DETAILS_SUCCESS: {
      return {
        ...state,
        chains: action.payload.chains,
        parsers: action.payload.parsers,
        routes: action.payload.routes,
        dirtyParsers: [],
        dirtyChains: [],
        path: [action.payload.chainId]
      };
    }
    case chainPageActions.REMOVE_PARSER: {
      const parsers = { ...state.parsers };
      delete parsers[action.payload.id];
      return {
        ...state,
        parsers,
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
        chains: {
          ...state.chains,
          [action.payload.chainId]: {
            ...state.chains[action.payload.chainId],
            parsers: (state.chains[action.payload.chainId].parsers as string[])
              .filter((parserId: string) => parserId !== action.payload.id)
          }
        }
      };
    }
    case chainPageActions.UPDATE_PARSER: {
      return {
        ...state,
        parsers: {
          ...state.parsers,
          [action.payload.parser.id]: {
            ...state.parsers[action.payload.parser.id],
            ...action.payload.parser
          }
        },
        dirtyParsers: uniqueAdd(state.dirtyParsers, action.payload.parser.id),
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
      };
    }
    case chainPageActions.ADD_CHAIN: {
      return {
        ...state,
        chains: {
          ...state.chains,
          [action.payload.chain.id]: {
            ...action.payload.chain as ChainModel,
            parsers: []
          }
        }
      };
    }
    case chainPageActions.UPDATE_CHAIN: {
      return {
        ...state,
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chain.id),
        chains: {
          ...state.chains,
          [action.payload.chain.id]: {
            ...state.chains[action.payload.chain.id],
            ...action.payload.chain
          }
        }
      };
    }
    case addParserActions.ADD_PARSER: {
      return {
        ...state,
        parsers: {
          ...state.parsers,
          [action.payload.parser.id]: action.payload.parser
        },
        dirtyParsers: uniqueAdd(state.dirtyParsers, action.payload.parser.id),
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
        chains: {
          ...state.chains,
          [action.payload.chainId]: {
            ...state.chains[action.payload.chainId],
            parsers: [
              ...(state.chains[action.payload.chainId].parsers as string[]),
              action.payload.parser.id
            ]
          }
        }
      };
    }
    case chainPageActions.INVESTIGATE_PARSER: {
      return {
        ...state,
        parserToBeInvestigated: action.payload.id
      };
    }
    case chainPageActions.GET_FORM_CONFIG_SUCCESS: {
      return {
        ...state,
        formConfigs: {
          ...(state.formConfigs || {}),
          [action.payload.parserType]: action.payload.formConfig
        }
      };
    }
    case chainPageActions.GET_FORM_CONFIGS_SUCCESS: {
      return {
        ...state,
        formConfigs: action.payload.formConfigs
      };
    }
    case chainPageActions.GET_INDEX_MAPPINGS_SUCCESS: {
      const { path, result } = action.payload;
      return {
        ...state,
        indexMappings: {path, result}
      };
    }
    case chainPageActions.SAVE_PARSER_CONFIG: {
      return {
        ...state,
        dirtyChains: [],
        dirtyParsers: []
      };
    }
    case chainPageActions.ADD_ROUTE: {
      return {
        ...state,
        dirtyParsers: uniqueAdd(state.dirtyParsers, action.payload.parserId),
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
        parsers: {
          ...state.parsers,
          [action.payload.parserId]: {
            ...state.parsers[action.payload.parserId],
            routing: {
              ...(state.parsers[action.payload.parserId].routing || {}),
              routes: [
                ...((state.parsers[action.payload.parserId].routing || {}).routes || []).filter(id => id !== action.payload.route.id),
                action.payload.route.id
              ]
            }
          }
        },
        routes: {
          ...state.routes,
          [action.payload.route.id]: {
            ...action.payload.route as RouteModel
          }
        }
      };
    }
    case chainPageActions.UPDATE_ROUTE: {
      return {
        ...state,
        dirtyParsers: uniqueAdd(state.dirtyParsers, action.payload.parserId),
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
        routes: {
          ...state.routes,
          [action.payload.route.id]: {
            ...state.routes[action.payload.route.id],
            ...action.payload.route
          }
        }
      };
    }
    case chainPageActions.REMOVE_ROUTE: {
      const routes = { ...state.routes };
      delete routes[action.payload.routeId];
      return {
        ...state,
        dirtyParsers: uniqueAdd(state.dirtyParsers, action.payload.parserId),
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
        routes,
        parsers: {
          ...state.parsers,
          [action.payload.parserId]: {
            ...state.parsers[action.payload.parserId],
            routing: {
              ...state.parsers[action.payload.parserId].routing,
              routes: state.parsers[action.payload.parserId].routing.routes.filter(id => id !== action.payload.routeId)
            }
          }
        }
      };
    }
    case chainPageActions.ADD_TO_PATH: {
      return {
        ...state,
        path: uniqueAdd(state.path, action.payload.chainId)
      };
    }
    case chainPageActions.REMOVE_FROM_PATH: {
      return {
        ...state,
        path: state.path.filter(chainId => !action.payload.chainId.includes(chainId))
      };
    }
    case chainPageActions.SET_ROUTE_AS_DEFAULT: {
      const routes = Object.keys(state.routes).reduce((acc, routeId) => {
        if (state.routes[routeId].default === true) {
          acc[routeId] = {
            ...state.routes[routeId],
            default: false
          };
        } else if (action.payload.routeId === routeId) {
          acc[routeId] = {
            ...state.routes[routeId],
            default: true
          };
        } else {
          acc[routeId] = state.routes[routeId];
        }
        return acc;
      }, {});
      return {
        ...state,
        dirtyParsers: uniqueAdd(state.dirtyParsers, action.payload.parserId),
        dirtyChains: uniqueAdd(state.dirtyChains, action.payload.chainId),
        routes
      };
    }
  }
  return state;
}

export function getChainPageState(state: any): ChainPageState {
  return state['chain-page'];
}

export const getChains = createSelector(
  getChainPageState,
  (state: ChainPageState): { [key: string]: ParserChainModel } => {
    return state.chains;
  }
);

export const getIndexMappings = createSelector(
  getChainPageState,
  (state: ChainPageState) => {
    return state.indexMappings;
  }
);

export const getChain = createSelector(
  getChainPageState,
  (state, props): ParserChainModel => {
    return state.chains[props.id];
  }
);

export const getParser = createSelector(
  getChainPageState,
  (state, props): ParserModel => {
    return state.parsers[props.id];
  }
);

export const getRoute = createSelector(
  getChainPageState,
  (state, props): RouteModel => {
    return state.routes[props.id];
  }
);

export const getChainDetails = createSelector(
  getChainPageState,
  (state, props) => {
    const mainChain = state.chains[props.chainId];
    return denormalizeParserConfig(mainChain, state);
  }
);

export const getParserToBeInvestigated = createSelector(
  getChainPageState,
  (state) => {
    return state.parserToBeInvestigated;
  }
);

export const getDirtyParsers = createSelector(
  getChainPageState,
  (state) => state.dirtyParsers
);

export const getDirtyChains = createSelector(
  getChainPageState,
  (state) => state.dirtyChains
);

export const getDirtyStatus = createSelector(
  getDirtyParsers,
  getDirtyChains,
  (parsers: string[], chains: string[]) => ({
    dirtyChains: chains,
    dirtyParsers: parsers
  })
);

export const getFormConfigByType = createSelector(
  getChainPageState,
  (state, props) => (state.formConfigs || {})[props.type]
);

export const getFormConfigs = createSelector(
  getChainPageState,
  (state) => state.formConfigs
);

export const getPath = createSelector(
  getChainPageState,
  (state) => state.path
);

export const getPathWithChains = createSelector(
  getPath,
  getChains,
  (path, chains) => path.map(chainId => chains[chainId])
);
