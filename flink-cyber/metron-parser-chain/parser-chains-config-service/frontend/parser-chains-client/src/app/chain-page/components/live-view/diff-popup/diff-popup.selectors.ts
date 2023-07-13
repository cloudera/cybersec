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

import {createSelector} from '@ngrx/store';
import {DiffPopupState} from "./diff-popup.reducers";

function getDiffPopupState(state: any): DiffPopupState {
    return state['diff-popup'];
}

export const getDiffModalVisible = createSelector(
    getDiffPopupState,
    (state: DiffPopupState) => state.diffModalShown
);

export const getPreviousDiffValue = createSelector(
    getDiffPopupState,
    (state: DiffPopupState) => state.previousDiffValue
);

export const getNewDiffValue = createSelector(
    getDiffPopupState,
    (state: DiffPopupState) => state.newDiffValue
);
