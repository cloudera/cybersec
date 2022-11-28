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
