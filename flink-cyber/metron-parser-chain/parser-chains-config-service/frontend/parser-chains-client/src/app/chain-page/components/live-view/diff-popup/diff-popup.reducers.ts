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

import {DiffPopupActionsType, HideDiffModalAction, ShowDiffModalAction} from "./diff-popup.actions";

export interface DiffPopupState {
    diffModalShown: boolean;
    previousDiffValue: string;
    newDiffValue: string;
}

export const initialState: DiffPopupState = {
    diffModalShown: false,
    previousDiffValue: "",
    newDiffValue: ""
};

export function reducer(
    state: DiffPopupState = initialState,
    action: DiffPopupActionsType
): DiffPopupState {
    switch (action.type) {
        case ShowDiffModalAction.type: {
            return {
                ...state,
                previousDiffValue: action.previousDiffValue,
                newDiffValue: action.newDiffValue,
                diffModalShown: true,
            };
        }
        case HideDiffModalAction.type: {
            return {
                ...state,
                diffModalShown: false,
            };
        }
        default: {
            return state;
        }
    }
}
