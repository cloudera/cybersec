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

import {
  ParserChainModel,
  ParserModel,
  PartialParserChainModel,
  PartialParserModel,
  PartialRouteModel,
  RouteModel
} from './chain-page.models';
import {CustomFormConfig} from './components/custom-form/custom-form.component';

export const LOAD_CHAIN_DETAILS = '[Chain Details] load start';
export const LOAD_CHAIN_DETAILS_SUCCESS = '[Chain Details] load success';
export const LOAD_CHAIN_DETAILS_FAIL = '[Chain Details] load fail';
export const REMOVE_PARSER = '[Chain Details] remove parser';
export const UPDATE_PARSER = '[Chain Details] update parser';
export const SAVE_PARSER_CONFIG = '[Chain Details] save parser config';
export const SAVE_PARSER_CONFIG_SUCCESS = '[Chain Details] save parser config success';
export const SAVE_PARSER_CONFIG_FAIL = '[Chain Details] save parser config fail';
export const ADD_CHAIN = '[Chain Details] add chain';
export const INVESTIGATE_PARSER = '[Chain Details] investigate parser';
export const UPDATE_CHAIN = '[Chain Details] update chain';
export const GET_FORM_CONFIG = '[Chain Details] get form config start';
export const GET_FORM_CONFIG_SUCCESS = '[Chain Details] get form config success';
export const GET_FORM_CONFIG_FAIL = '[Chain Details] get form config fail';
export const GET_FORM_CONFIGS = '[Chain Details] get form configs start';
export const GET_FORM_CONFIGS_SUCCESS = '[Chain Details] get form configs success';
export const GET_FORM_CONFIGS_FAIL = '[Chain Details] get form configs fail';
export const GET_INDEX_MAPPINGS = '[Index Mappings] get index mappings start';
export const GET_INDEX_MAPPINGS_SUCCESS = '[Index Mappings] get index mappings success';
export const GET_INDEX_MAPPINGS_FAIL = '[Index Mappings] get index mappings fail';
export const ADD_ROUTE = '[Chain Details] add route';
export const UPDATE_ROUTE = '[Chain Details] update route';
export const REMOVE_ROUTE = '[Chain Details] remove route';
export const SET_ROUTE_AS_DEFAULT = '[Chain Details] set route as default';
export const ADD_TO_PATH = '[Chain Details] add to path';
export const REMOVE_FROM_PATH = '[Chain Details] remove from path';

export class LoadChainDetailsAction implements Action {
  readonly type = LOAD_CHAIN_DETAILS;
  constructor(public payload: { id: string }) {}
}

export class LoadChainDetailsSuccessAction implements Action {
  readonly type = LOAD_CHAIN_DETAILS_SUCCESS;
  constructor(public payload: {
    chains: { [key: string]: ParserChainModel },
    routes: { [key: string]: RouteModel },
    parsers: { [key: string]: ParserModel },
    chainId: string
  }) {}
}

export class LoadChainDetailsFailAction implements Action {
  readonly type = LOAD_CHAIN_DETAILS_FAIL;
  constructor(public error: { message: string }) {}
}

export class RemoveParserAction implements Action {
  readonly type = REMOVE_PARSER;
  constructor(public payload: { id: string, chainId: string }) {}
}

export class UpdateParserAction implements Action {
  readonly type = UPDATE_PARSER;
  constructor(public payload: {
    chainId: string,
    parser: PartialParserModel
  }) {}
}

export class SaveParserConfigAction implements Action {
  readonly type = SAVE_PARSER_CONFIG;
  constructor(public payload: { chainId: string }) {}
}

export class SaveParserConfigSuccessAction implements Action {
  readonly type = SAVE_PARSER_CONFIG_SUCCESS;
  constructor() {}
}

export class SaveParserConfigFailAction implements Action {
  readonly type = SAVE_PARSER_CONFIG_FAIL;
  constructor(public error: { message: string }) {}
}

export class AddChainAction implements Action {
  readonly type = ADD_CHAIN;
  constructor(public payload: { chain: PartialParserChainModel }) {}
}

export class InvestigateParserAction implements Action {
  readonly type = INVESTIGATE_PARSER;
  constructor(public payload: { id: string }) {}
}

export class UpdateChainAction implements Action {
  readonly type = UPDATE_CHAIN;
  constructor(public payload: { chain: PartialParserChainModel }) {}
}

export class GetFormConfigAction implements Action {
  readonly type = GET_FORM_CONFIG;
  constructor(public payload: { type: string }) {}
}

export class GetFormConfigSuccessAction implements Action {
  readonly type = GET_FORM_CONFIG_SUCCESS;
  constructor(public payload: { parserType: string, formConfig: CustomFormConfig[] }) {}
}

export class GetFormConfigFailAction implements Action {
  readonly type = GET_FORM_CONFIG_FAIL;
  constructor(public error: { message: string }) {}
}

export class GetFormConfigsAction implements Action {
  readonly type = GET_FORM_CONFIGS;
  constructor() {}
}

export class GetFormConfigsSuccessAction implements Action {
  readonly type = GET_FORM_CONFIGS_SUCCESS;
  constructor(public payload: { formConfigs: { [key: string]: CustomFormConfig[] } }) {}
}

export class GetFormConfigsFailAction implements Action {
  readonly type = GET_FORM_CONFIGS_FAIL;
  constructor(public error: { message: string }) {}
}

export class GetIndexMappingsAction implements Action {
  readonly type = GET_INDEX_MAPPINGS;
  constructor(public payload?: { filePath: string }) {}
}

export class GetIndexMappingsSuccessAction implements Action {
  readonly type = GET_INDEX_MAPPINGS_SUCCESS;
  constructor(public payload: { path: string, result: Map<string, object> }) {}
}

export class GetIndexMappingsFailAction implements Action {
  readonly type = GET_INDEX_MAPPINGS_FAIL;
  constructor(public error: { message: string }) {}
}

export class AddRouteAction implements Action {
  readonly type = ADD_ROUTE;
  constructor(public payload: {
    chainId: string,
    parserId: string,
    route: PartialRouteModel
  }) {}
}

export class UpdateRouteAction implements Action {
  readonly type = UPDATE_ROUTE;
  constructor(public payload: {
    chainId: string,
    parserId: string,
    route: PartialRouteModel
  }) {}
}

export class RemoveRouteAction implements Action {
  readonly type = REMOVE_ROUTE;
  constructor(public payload: {
    chainId: string,
    parserId: string,
    routeId: string
  }) {}
}

export class AddToPathAction implements Action {
  readonly type = ADD_TO_PATH;
  constructor(public payload: { chainId: string }) {}
}

export class RemoveFromPathAction implements Action {
  readonly type = REMOVE_FROM_PATH;
  constructor(public payload: { chainId: string[] }) {}
}

export class SetRouteAsDefaultAction implements Action {
  readonly type = SET_ROUTE_AS_DEFAULT;
  constructor(public payload: {
    chainId: string,
    parserId: string,
    routeId: string
  }) {}
}

export type ChainDetailsAction = LoadChainDetailsAction
  | LoadChainDetailsSuccessAction
  | LoadChainDetailsFailAction
  | RemoveParserAction
  | UpdateParserAction
  | SaveParserConfigAction
  | SaveParserConfigSuccessAction
  | SaveParserConfigFailAction
  | AddChainAction
  | InvestigateParserAction
  | UpdateChainAction
  | GetFormConfigAction
  | GetFormConfigSuccessAction
  | GetFormConfigFailAction
  | GetFormConfigsAction
  | GetFormConfigsSuccessAction
  | GetFormConfigsFailAction
  | AddRouteAction
  | UpdateRouteAction
  | RemoveRouteAction
  | SetRouteAsDefaultAction
  | AddToPathAction
  | RemoveFromPathAction;

export type IndexMappingAction = GetIndexMappingsAction
    | GetIndexMappingsSuccessAction
    | GetIndexMappingsFailAction;
