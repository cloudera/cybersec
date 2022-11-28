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

import { createSelector } from '@ngrx/store';

import {
  executionTriggered,
  LiveViewActionsType,
  liveViewRefreshedSuccessfully,
  liveViewRefreshFailed,
  onOffToggleChanged,
  onOffToggleRestored,
  sampleDataInputChanged,
  sampleDataRestored,
} from './live-view.actions';
import { EntryParsingResultModel } from './models/live-view.model';
import { SampleDataModel, SampleDataType } from './models/sample-data.model';

export interface LiveViewState {
  isLiveViewOn: boolean;
  isExecuting: boolean;
  sampleData: SampleDataModel;
  result: EntryParsingResultModel[];
  failedParser?: any;
}

export const initialState: LiveViewState = {
  isLiveViewOn: false,
  isExecuting: false,
  sampleData: {
    type: SampleDataType.MANUAL,
    source: '',
  },
  result: [],
  failedParser: ''
};

export function reducer(
  state: LiveViewState = initialState,
  action: LiveViewActionsType
): LiveViewState {
  switch (action.type) {
    case executionTriggered.type: {
      return {
        ...state,
        sampleData: action.sampleData,
        isExecuting: true,
      };
    }
    case liveViewRefreshedSuccessfully.type: {
      const failedResult = (action.liveViewResult.results.find((result) => result.log.type === 'error'));
      return {
        ...state,
        isExecuting: false,
        result: action.liveViewResult.results,
        failedParser: failedResult === undefined ? '' : failedResult.log
      };
    }
    case liveViewRefreshFailed.type: {
      return {
        ...state,
        isExecuting: false,
      };
    }
    case onOffToggleChanged.type: {
      return {
        ...state,
        isLiveViewOn: action.value,
      };
    }
    case onOffToggleRestored.type: {
      if (action.value === null) { return state; }

      return {
        ...state,
        isLiveViewOn: action.value
      };
    }
    case sampleDataInputChanged.type: {
      return {
        ...state,
        sampleData: action.sampleData,
      };
    }
    case sampleDataRestored.type: {
      if (action.sampleData === null) { return state; }

      return {
        ...state,
        sampleData: action.sampleData
      };
    }
    default: {
      return state;
    }
  }
}

export function getLiveViewState(state: any): LiveViewState {
  return state['live-view'];
}

export const getFailedParser = createSelector(
  getLiveViewState,
  (state: LiveViewState) => state.failedParser.parserId
);
