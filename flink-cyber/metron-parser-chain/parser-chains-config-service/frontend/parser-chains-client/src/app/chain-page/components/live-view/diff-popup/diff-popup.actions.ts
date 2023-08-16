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

import {createAction, props, union} from '@ngrx/store';


export const ShowDiffModalAction = createAction(
    '[DiffModal] Diff Modal Shown',
    props<{ previousDiffValue: string, newDiffValue: string }>()
);

export const HideDiffModalAction = createAction(
    '[DiffModal] Diff Modal Hidden'
);

const actions = union({
    ShowDiffModalAction,
    HideDiffModalAction
});

export type DiffPopupActionsType = typeof actions;
