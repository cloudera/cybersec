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
import {SampleDataInternalModel} from "../../models/sample-data.model";
import {EntryParsingResultModel} from "../../models/live-view.model";
import {ChainDetailsModel} from "../../../../chain-page.models";

export const SampleFolderViewInitializedAction = createAction(
  '[SampleFolder] Sample Folder View Initialized'
);

export const SampleFolderPathRestoredAction = createAction(
  '[SampleFolder] Sample Folder Path Restored',
    props<{ sampleFolderPath: string }>()
);

export const ExecutionListTriggeredAction = createAction(
  '[SampleFolder] Sample Data List Parsing Triggered',
  props<{ sampleData: SampleDataInternalModel[], chainConfig: ChainDetailsModel }>()
);

export const ExecutionListSuccessfulAction = createAction(
  '[SampleFolder] Sample Data List Tested Successfully',
  props<{ sampleFolderResults: Map<number, [SampleDataInternalModel, EntryParsingResultModel[]]> }>()
);

export const ExecutionListFailedAction = createAction(
  '[SampleFolder] Sample Data List Test Failed',
  props<{ error: { message: string } }>()
);

export const ShowEditModalAction = createAction(
    '[SampleFolder] Sample Edit Modal Shown'
);

export const HideEditModalAction = createAction(
    '[SampleFolder] Sample Edit Modal Hidden'
);

export const SaveSampleListTriggeredAction = createAction(
    '[SampleFolder] Save Sample List Triggered',
    props<{ folderPath: string, chainId: string, sampleList: SampleDataInternalModel[] }>()
);

export const SaveSampleListSuccessfulAction = createAction(
    '[SampleFolder] Save Sample List Successful',
  props<{ saveResults: SampleDataInternalModel[] }>()
);

export const SaveSampleListFailedAction = createAction(
    '[SampleFolder] Save Sample List Failed',
  props<{ error: { message: string } }>()
);

export const FetchSampleListTriggeredAction = createAction(
    '[SampleFolder] Fetch Sample List Triggered',
    props<{ folderPath: string, chainId: string }>()
);

export const FetchSampleListSuccessfulAction = createAction(
    '[SampleFolder] Fetch Sample List Successful',
    props<{ fetchResult: SampleDataInternalModel[] }>()
);

export const FetchSampleListFailedAction = createAction(
    '[SampleFolder] Fetch Sample List Failed',
  props<{ error: { message: string } }>()
);

const actions = union({
  SampleFolderViewInitializedAction,
  SampleFolderPathRestoredAction,
  ExecutionListTriggeredAction,
  ExecutionListSuccessfulAction,
  ExecutionListFailedAction,
  ShowEditModalAction,
  HideEditModalAction,
  SaveSampleListTriggeredAction,
  SaveSampleListSuccessfulAction,
  SaveSampleListFailedAction,
  FetchSampleListTriggeredAction,
  FetchSampleListSuccessfulAction,
  FetchSampleListFailedAction
});

export type SampleFolderActionsType = typeof actions;
