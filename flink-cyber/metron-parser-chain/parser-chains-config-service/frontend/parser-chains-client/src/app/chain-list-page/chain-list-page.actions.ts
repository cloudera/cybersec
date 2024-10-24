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

import {Action} from '@ngrx/store';

import {ChainModel, ChainOperationalModel} from './chain.model';

export const LOAD_CHAINS = '[Chain List] load all start';
export const LOAD_CHAINS_SUCCESS = '[Chain List] load all success';
export const LOAD_CHAINS_FAIL = '[Chain List] load all fail';
export const DELETE_CHAIN = '[Chain List] delete item';
export const DELETE_CHAIN_SELECT = '[Chain List] prepare delete item';
export const DELETE_CHAIN_SUCCESS = '[Chain List] delete item success';
export const DELETE_CHAIN_FAIL = '[Chain List] delete item fail';
export const CREATE_CHAIN = '[Chain List] create item';
export const SHOW_CREATE_MODAL = '[Chain List] show create modal';
export const HIDE_CREATE_MODAL = '[Chain List] hide create modal';
export const SHOW_DELETE_MODAL = '[Chain List] show delete modal';
export const HIDE_DELETE_MODAL = '[Chain List] hide delete modal';
export const SHOW_RENAME_SELECTED_PIPELINE_MODAL = '[Chain List] show rename selected pipeline modal';
export const HIDE_RENAME_PIPELINE_MODAL = '[Chain List] hide rename pipeline modal';
export const CREATE_CHAIN_SUCCESS = '[Chain List] create item success';
export const CREATE_CHAIN_FAIL = '[Chain List] create item fail';
export const LOAD_PIPELINES = '[Chain List] load pipelines start';
export const LOAD_PIPELINES_SUCCESS = '[Chain List] load pipelines success';
export const LOAD_PIPELINES_FAIL = '[Chain List] load pipelines fail';
export const CREATE_PIPELINE = '[Chain List] create pipeline start';
export const CREATE_PIPELINE_SUCCESS = '[Chain List] create pipeline success';
export const CREATE_PIPELINE_FAIL = '[Chain List] create pipeline fail';
export const RENAME_SELECTED_PIPELINE = '[Chain List] rename selected pipeline start';
export const RENAME_PIPELINE_SUCCESS = '[Chain List] rename pipeline success';
export const RENAME_PIPELINE_FAIL = '[Chain List] rename pipeline fail';
export const DELETE_SELECTED_PIPELINE = '[Chain List] delete selected pipeline start';
export const DELETE_PIPELINE_SUCCESS = '[Chain List] delete pipeline success';
export const DELETE_PIPELINE_FAIL = '[Chain List] delete pipeline fail';
export const PIPELINE_CHANGED = '[Chain List] selected pipeline changed';

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
  constructor(public chainId: string, public chainName: string) {}
}
export class SelectDeleteChainAction implements Action {
  readonly type = DELETE_CHAIN_SELECT;
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

export class ShowCreateModalAction implements Action {
    readonly type = SHOW_CREATE_MODAL;
    constructor() {}
}

export class HideCreateModalAction implements Action {
    readonly type = HIDE_CREATE_MODAL;
    constructor() {}
}

export class ShowDeleteModalAction implements Action {
  readonly type = SHOW_DELETE_MODAL;
  constructor() {}
}

export class HideDeleteModalAction implements Action {
  readonly type = HIDE_DELETE_MODAL;
  constructor() {}
}

export class ShowRenameSelectedPipelineModalAction implements Action {
  readonly type = SHOW_RENAME_SELECTED_PIPELINE_MODAL;
  constructor() {}
}

export class HideRenamePipelineModalAction implements Action {
  readonly type = HIDE_RENAME_PIPELINE_MODAL;
  constructor() {}
}

export class CreateChainSuccessAction implements Action {
    readonly type = CREATE_CHAIN_SUCCESS;
    constructor(public chain: ChainModel) {}
  }

export class CreateChainFailAction implements Action {
    readonly type = CREATE_CHAIN_FAIL;
    constructor(public error: { message: string }) {}
  }

export class LoadPipelinesAction implements Action {
    readonly type = LOAD_PIPELINES;
    constructor() {}
  }

export class LoadPipelinesSuccessAction implements Action {
    readonly type = LOAD_PIPELINES_SUCCESS;
    constructor(public pipelines: string[]) {}
  }

export class LoadPipelinesFailAction implements Action {
    readonly type = LOAD_PIPELINES_FAIL;
    constructor(public error: { message: string }) {}
  }

export class CreatePipelineAction implements Action {
    readonly type = CREATE_PIPELINE;
    constructor(public pipelineName: string) {}
  }

export class CreatePipelineSuccessAction implements Action {
    readonly type = CREATE_PIPELINE_SUCCESS;
    constructor(public newPipelineName, public pipelines: string[]) {}
  }

export class CreatePipelineFailAction implements Action {
    readonly type = CREATE_PIPELINE_FAIL;
    constructor(public error: { message: string }) {}
  }

export class RenameSelectedPipelineAction implements Action {
    readonly type = RENAME_SELECTED_PIPELINE;
    constructor(public newPipelineName: string) {}
  }

export class RenamePipelineSuccessAction implements Action {
    readonly type = RENAME_PIPELINE_SUCCESS;
    constructor(public newPipelineName, public pipelines: string[]) {}
  }

export class RenamePipelineFailAction implements Action {
    readonly type = RENAME_PIPELINE_FAIL;
    constructor(public error: { message: string }) {}
  }

export class DeleteSelectedPipelineAction implements Action {
    readonly type = DELETE_SELECTED_PIPELINE;
    constructor() {}
  }

export class DeletePipelineSuccessAction implements Action {
    readonly type = DELETE_PIPELINE_SUCCESS;
    constructor(public pipelines: string[]) {}
  }

export class DeletePipelineFailAction implements Action {
    readonly type = DELETE_PIPELINE_FAIL;
    constructor(public error: { message: string }) {}
  }

export class PipelineChangedAction implements Action {
    readonly type = PIPELINE_CHANGED;
    constructor(public newPipelineName: string) {}
}

export type ChainListAction = LoadChainsAction
  | LoadChainsSuccessAction
  | LoadChainsFailAction
  | ShowDeleteModalAction
  | HideDeleteModalAction
  | SelectDeleteChainAction
  | DeleteChainAction
  | DeleteChainSuccessAction
  | DeleteChainFailAction
  | CreateChainAction
  | ShowCreateModalAction
  | HideCreateModalAction
  | ShowRenameSelectedPipelineModalAction
  | HideRenamePipelineModalAction
  | CreateChainSuccessAction
  | CreateChainFailAction
  | NoopChainAction
  | LoadPipelinesAction
  | LoadPipelinesSuccessAction
  | LoadPipelinesFailAction
  | CreatePipelineAction
  | CreatePipelineSuccessAction
  | CreatePipelineFailAction
  | RenameSelectedPipelineAction
  | RenamePipelineSuccessAction
  | RenamePipelineFailAction
  | DeleteSelectedPipelineAction
  | DeletePipelineSuccessAction
  | DeletePipelineFailAction
  | PipelineChangedAction;
