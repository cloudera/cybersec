/*
 * Copyright 2020 - 2023 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import {
  ExecutionListFailedAction,
  ExecutionListSuccessfulAction,
  ExecutionListTriggeredAction,
  FetchSampleListFailedAction,
  FetchSampleListSuccessfulAction,
  FetchSampleListTriggeredAction,
  SampleFolderActionsType, SampleFolderPathRestoredAction,
  SampleFolderViewInitializedAction,
  SaveSampleListFailedAction,
  SaveSampleListSuccessfulAction,
  SaveSampleListTriggeredAction
} from "./sample-data-text-folder-input.actions";
import {SampleDataTextFolderInputService} from "../../services/sample-data-text-folder-input.service";
import {SampleFolderConsts} from "./sample-data-text-folder-input.consts";
import {LiveViewConsts} from "../../live-view.consts";

@Injectable()
export class SampleDataTextFolderInputEffects {
  constructor(
    private actions$: Actions<SampleFolderActionsType>,
    private sampleFolderService: SampleDataTextFolderInputService,
    private messageService: NzMessageService,
  ) {}

  @Effect()
  init$: Observable<Action> = this.actions$.pipe(
    ofType(
      SampleFolderViewInitializedAction.type,
    ),
      switchMap(() => {
        const sampleFolderPath = localStorage.getItem(SampleFolderConsts.SAMPLE_FOLDER_PATH_STORAGE_KEY);
        return of(SampleFolderPathRestoredAction({ sampleFolderPath }));
      })
  );

  @Effect()
  runTests$: Observable<Action> = this.actions$.pipe(
    ofType(
      ExecutionListTriggeredAction.type,
    ),
    switchMap(({ sampleData, chainConfig }) => {
      return this.sampleFolderService.runTests(sampleData, chainConfig).pipe(
        map(sampleFolderResults => ExecutionListSuccessfulAction({ sampleFolderResults })),
        catchError(( error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(ExecutionListFailedAction({ error }));
        })
      );
    })
  );

  @Effect()
  fetchSamples$: Observable<Action> = this.actions$.pipe(
    ofType(
      FetchSampleListTriggeredAction.type,
    ),
    switchMap(({ folderPath, chainId }) => {
      localStorage.setItem(SampleFolderConsts.SAMPLE_FOLDER_PATH_STORAGE_KEY, folderPath);

      return this.sampleFolderService.fetchSamples(folderPath, chainId).pipe(
        map(fetchResult => FetchSampleListSuccessfulAction({ fetchResult })),
        catchError(( error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(FetchSampleListFailedAction({ error }));
        })
      );
    })
  );

  @Effect()
  saveSamples$: Observable<Action> = this.actions$.pipe(
    ofType(
      SaveSampleListTriggeredAction.type,
    ),
    switchMap(({ folderPath, chainId, sampleList }) => {
      return this.sampleFolderService.saveSamples(folderPath, chainId, sampleList).pipe(
        map(saveResults => SaveSampleListSuccessfulAction({ saveResults })),
        catchError(( error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(SaveSampleListFailedAction({ error }));
        })
      );
    })
  );

}
