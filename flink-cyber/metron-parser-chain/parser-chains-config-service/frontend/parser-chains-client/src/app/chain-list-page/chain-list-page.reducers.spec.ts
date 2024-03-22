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
import * as fromActions from './chain-list-page.actions';
import * as fromReducers from './chain-list-page.reducers';
import {ChainListPageState} from "./chain-list-page.reducers";


describe('chain-list-page: reducers', () => {
  it('should return with the initial state.', () => {
    expect(fromReducers.reducer(undefined, new fromActions.NoopChainAction())).toEqual(fromReducers.initialState);
  });

  it('should return with the previous state', () => {
    const previousState: ChainListPageState = { items: [], createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: false, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.NoopChainAction())).toEqual(previousState);
  });

  it('should set loading true', () => {
    const previousState: ChainListPageState = { items: [], createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: false, error: ''};
    expect(fromReducers.reducer(previousState, new fromActions.LoadChainsAction())
    ).toEqual({
      ...previousState,
      loading: true,
    });
  });

  it('should set the error message and set loading false', () => {
    const previousState: ChainListPageState = { items: [], createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: true, error: ''};
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
    const previousState: ChainListPageState = { items, createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: true, error: ''};
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

    const previousState: ChainListPageState = { items: itemList, createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: true, error: ''};

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
    const previousState: ChainListPageState = { items: [], createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: true, error: ''};
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

    const previousState: ChainListPageState = { items: itemList, createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: true, error: ''};

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
    const previousState: ChainListPageState = { items: [], createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: true, error: ''};
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
    const expected: ChainListPageState = { items: [], createModalVisible: false, deleteModalVisible: false, deleteItem: null, loading: false, error: ''};

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
