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

import { createAction, props, union } from '@ngrx/store';

import { EntryParsingResultModel } from './models/live-view.model';
import { SampleDataModel } from './models/sample-data.model';

export const liveViewInitialized = createAction(
  '[LiveView] Live View Initialized'
);

export const executionTriggered = createAction(
  '[LiveView] Sample Data Parsing Triggered',
  props<{ sampleData: SampleDataModel, chainConfig: unknown }>()
);

export const liveViewRefreshedSuccessfully = createAction(
  '[LiveView] Live View Refreshed Successfully',
  props<{ liveViewResult: { results: EntryParsingResultModel[]}  }>()
);

export const liveViewRefreshFailed = createAction(
  '[LiveView] Live View Refresh Failed',
  props<{ error: { message: string } }>()
);

export const sampleDataInputChanged = createAction(
  '[LiveView] Sample Data Input Changed',
  props<{ sampleData: SampleDataModel }>()
);

export const onOffToggleChanged = createAction(
  '[LiveView] On/Off Toggle Changed',
  props<{ value: boolean }>()
);

export const sampleDataRestored = createAction(
  '[LiveView] Live View Sample Data Restored',
  props<{ sampleData: SampleDataModel }>()
);

export const onOffToggleRestored = createAction(
  '[LiveView] On/Off Toggle Restored',
  props<{ value: boolean }>()
);

const actions = union({
  liveViewInitialized,
  executionTriggered,
  liveViewRefreshedSuccessfully,
  liveViewRefreshFailed,
  onOffToggleChanged,
  sampleDataInputChanged,
  onOffToggleRestored,
  sampleDataRestored,
});

export type LiveViewActionsType = typeof actions;
