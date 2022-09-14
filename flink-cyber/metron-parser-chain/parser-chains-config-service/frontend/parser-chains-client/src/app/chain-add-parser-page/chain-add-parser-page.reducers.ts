import { createSelector } from '@ngrx/store';

import * as addParserActions from './chain-add-parser-page.actions';

export interface AddParserPageState {
  parserTypes: { id: string, name: string }[];
  error: string;
}

export const initialState: AddParserPageState = {
  parserTypes: [],
  error: '',
};

export function reducer(
  state: AddParserPageState = initialState,
  action: addParserActions.ParserAction
): AddParserPageState {
  switch (action.type) {
    case addParserActions.GET_PARSER_TYPES_SUCCESS: {
      return {
        ...state,
        parserTypes: [
          ...(action.payload || []),
          {
            id: 'Router',
            name: 'Router'
          }
        ]
      };
    }
  }
  return state;
}

function getChainAddParserPageState(state: any): AddParserPageState {
  return state['chain-add-parser-page'];
}

export const getParserTypes = createSelector(
  getChainAddParserPageState,
  (state: AddParserPageState) => state.parserTypes
);
