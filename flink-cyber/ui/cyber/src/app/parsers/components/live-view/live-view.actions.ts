import { createAction, props, union } from '@ngrx/store';

import { EntryParsingResultModel } from './models/live-view.model';
import { SampleDataModel } from './models/sample-data.model';

export const liveViewInitialized = createAction(
  '[LiveView] Live View Initialized'
);

export const executionTriggered = createAction(
  '[LiveView] Sample Data Parsing Triggered',
  props<{ sampleData: SampleDataModel, chainConfig: {} }>()
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
