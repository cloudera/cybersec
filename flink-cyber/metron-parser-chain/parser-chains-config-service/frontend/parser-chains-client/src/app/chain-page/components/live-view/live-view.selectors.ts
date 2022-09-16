import { createSelector } from '@ngrx/store';

import { LiveViewState } from './live-view.reducers';

function getLiveViewState(state: any): LiveViewState {
  return state['live-view'];
}

export const getSampleData = createSelector(
  getLiveViewState,
  (state: LiveViewState) => state.sampleData
);

export const getResults = createSelector(
  getLiveViewState,
  (state: LiveViewState) => state.result
);

export const getExecutionStatus = createSelector(
  getLiveViewState,
  (state: LiveViewState) => state.isExecuting
);

export const getIsLiveViewOn = createSelector(
  getLiveViewState,
  (state: LiveViewState) => state.isLiveViewOn
);

