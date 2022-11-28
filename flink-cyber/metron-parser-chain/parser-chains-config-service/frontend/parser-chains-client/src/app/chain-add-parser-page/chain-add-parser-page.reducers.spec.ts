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

import * as fromActions from './chain-add-parser-page.actions';

import * as fromReducers from './chain-add-parser-page.reducers';

describe('chain add parser page: reducers', () => {

  it('should return with the initial state.', () => {
    expect(fromReducers.reducer(undefined, new fromActions.NoopAction())).toBe(fromReducers.initialState);
  });

  it('should set the parser types and append Router', () => {
    const state = {} as unknown;
    const parserTypes = [{
      id: 'Parser',
      name: 'Parser'
    }];
    const newState = fromReducers.reducer(
      state as fromReducers.AddParserPageState,
      new fromActions.GetParserTypesSuccessAction(parserTypes)
    );
    expect(newState.parserTypes).toEqual([
      ...parserTypes,
      { id: 'Router', name: 'Router' }
    ]);
  });
});

describe('chain add parser page: selectors', () => {

  it('should return with the parser types from state', () => {
    const parserTypesFromState = [];
    const parserTypes = fromReducers.getParserTypes({
      'chain-add-parser-page': {
        parserTypes: parserTypesFromState
      }
    });
    expect(parserTypes).toBe(parserTypesFromState);
  });
});
