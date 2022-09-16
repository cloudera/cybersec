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
