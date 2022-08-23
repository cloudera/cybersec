import { Action } from '@ngrx/store';

import { ChainModel, ChainOperationalModel } from './chain.model';

export const LOAD_CHAINS = '[Chain List] load all start';
export const LOAD_CHAINS_SUCCESS = '[Chain List] load all success';
export const LOAD_CHAINS_FAIL = '[Chain List] load all fail';
export const DELETE_CHAIN = '[Chain List] delete item';
export const DELETE_CHAIN_SUCCESS = '[Chain List] delete item success';
export const DELETE_CHAIN_FAIL = '[Chain List] delete item fail';
export const CREATE_CHAIN = '[Chain List] create item';
export const CREATE_CHAIN_SUCCESS = '[Chain List] create item success';
export const CREATE_CHAIN_FAIL = '[Chain List] create item fail';

export class NoopChainAction implements Action {
  readonly type: '';
  constructor(public payload?: any) {}
}
export class LoadChainsAction implements Action {
  readonly type = LOAD_CHAINS;
  constructor() {}
}

export class LoadChainsSuccessAction implements Action {
  readonly type = LOAD_CHAINS_SUCCESS;
  constructor(public chains: ChainModel[]) {}
}

export class LoadChainsFailAction implements Action {
  readonly type = LOAD_CHAINS_FAIL;
  constructor(public error: { message: string }) {}
}

export class DeleteChainAction implements Action {
  readonly type = DELETE_CHAIN;
  constructor(public chainId: string) {}
}

export class DeleteChainSuccessAction implements Action {
  readonly type = DELETE_CHAIN_SUCCESS;
  constructor(public chainId: string) {}
}

export class DeleteChainFailAction implements Action {
  readonly type = DELETE_CHAIN_FAIL;
  constructor(public error: { message: string }) {}
}

export class CreateChainAction implements Action {
    readonly type = CREATE_CHAIN;
    constructor(public newChain: ChainOperationalModel) {}
  }

export class CreateChainSuccessAction implements Action {
    readonly type = CREATE_CHAIN_SUCCESS;
    constructor(public chain: ChainModel) {}
  }

export class CreateChainFailAction implements Action {
    readonly type = CREATE_CHAIN_FAIL;
    constructor(public error: { message: string }) {}
  }

export type ChainListAction = LoadChainsAction
  | LoadChainsSuccessAction
  | LoadChainsFailAction
  | DeleteChainAction
  | DeleteChainSuccessAction
  | DeleteChainFailAction
  | CreateChainAction
  | CreateChainSuccessAction
  | CreateChainFailAction
  | NoopChainAction;
