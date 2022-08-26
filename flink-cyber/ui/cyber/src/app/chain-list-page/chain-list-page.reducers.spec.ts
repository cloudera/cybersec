import * as fromActions from './chain-list-page.actions';

import * as fromReducers from './chain-list-page.reducers';

describe('chain-list-page: reducers', () => {
  it('should return with the initial state.', () => {
    expect(fromReducers.reducer(undefined, new fromActions.NoopChainAction())).toBe(fromReducers.initialState);
  });

  it('should return with the previous state', () => {
    const previousState = { items: [], loading: false, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.NoopChainAction())).toBe(previousState);
  });

  it('should set loading true', () => {
    const previousState = { items: [], loading: false, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.LoadChainsAction())
    ).toEqual({
      ...previousState,
      loading: true,
    });
  });

  it('should set the error message and set loading false', () => {
    const previousState = { items: [], loading: true, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.LoadChainsFailAction({
      message: 'Something went wrong.'
    }))
    ).toEqual({
      ...previousState,
      loading: false,
      error: 'Something went wrong.'
    });
  });

  it('should set the items and set loading false', () => {
    const items = [{
      id: 'id1',
      name: 'Chain 1'
    }, {
      id: 'id2',
      name: 'Chain 2'
    }];
    const previousState = { items, loading: true, error: ''};
    expect(
      fromReducers.reducer(previousState, new fromActions.LoadChainsSuccessAction(items))
    ).toEqual({
      ...previousState,
      loading: false,
      items
    });
  });

  it('should delete the chain', () => {
    const itemList = [{
        id: 'id1',
        name: 'Chain 1'
    }, {
        id: 'id2',
        name: 'Chain 2'
    }];

    const previousState = { items: itemList, loading: true, error: ''};

    expect(
      fromReducers.reducer(previousState, new fromActions.DeleteChainSuccessAction('id2'))
    ).toEqual({
      ...previousState,
      loading: false,
      items: [{
        id: 'id1',
        name: 'Chain 1'
      }]
    });
  });

  it('should set the delete error message and set loading false', () => {
    const previousState = { items: [], loading: true, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.DeleteChainFailAction({
      message: 'Something went wrong.'
    }))
    ).toEqual({
      ...previousState,
      loading: false,
      error: 'Something went wrong.'
    });
  });

  it('should create a chain', () => {
    const itemList = [{
        id: 'id1',
        name: 'Chain 1'
    }];

    const previousState = { items: itemList, loading: true, error: ''};

    expect(
      fromReducers.reducer(previousState, new fromActions.CreateChainSuccessAction({id: 'id2', name: 'Chain 2'}))
    ).toEqual({
      ...previousState,
      loading: false,
      items: [{
        id: 'id1',
        name: 'Chain 1'
      }, {
        id: 'id2',
        name: 'Chain 2'
      }]
    });
  });

  it('should set the create error message and set loading false', () => {
    const previousState = { items: [], loading: true, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.CreateChainFailAction({
      message: 'Something went wrong.'
    }))
    ).toEqual({
      ...previousState,
      loading: false,
      error: 'Something went wrong.'
    });
  });
});

describe('chain-list-page: selectors', () => {
  it('should return with substate', () => {
    const expected = {
      items: [],
      loading: false,
      error: ''
    };
    const state = { 'chain-list-page': expected };
    expect(fromReducers.getChainListPageState(state)).toEqual(expected);
  });

  it('should return with types', () => {
    const expected = [];
    const state = {
      'chain-list-page': {
        items: expected
      }
    };
    expect(fromReducers.getChains(state)).toEqual(expected);
  });
});
