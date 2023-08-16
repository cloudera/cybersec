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

import {SampleDataTextFolderInputState} from "./sample-data-text-folder-input.reducers";

function getSampleFolderInputState(state: any): SampleDataTextFolderInputState {
  return state['sample-folder'];
}

export const getSampleData = createSelector(
    getSampleFolderInputState,
  (state: SampleDataTextFolderInputState) => state.sampleData
);

export const getSampleFolderPath = createSelector(
    getSampleFolderInputState,
  (state: SampleDataTextFolderInputState) => state.sampleFolderPath
);

export const getRunResults = createSelector(
    getSampleFolderInputState,
  (state: SampleDataTextFolderInputState) => state.runResult
);

export const getExecutionStatus = createSelector(
    getSampleFolderInputState,
  (state: SampleDataTextFolderInputState) => state.isTestExecuting
);

export const getEditModalVisible = createSelector(
    getSampleFolderInputState,
  (state: SampleDataTextFolderInputState) => state.editModalShown
);
