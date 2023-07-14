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

import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';

import {
  executionTriggered,
  LiveViewActionsType,
  liveViewInitialized,
  liveViewRefreshedSuccessfully,
  liveViewRefreshFailed,
  onOffToggleChanged,
  onOffToggleRestored,
  sampleDataInputChanged,
  sampleDataRestored,
} from './live-view.actions';
import { LiveViewConsts } from './live-view.consts';
import { LiveViewService } from './services/live-view.service';
import {ClusterService} from "../../../services/cluster.service";

@Injectable()
export class LiveViewEffects {
  constructor(
    private actions$: Actions<LiveViewActionsType>,
    private liveViewService: LiveViewService,
    private messageService: NzMessageService,
    private clusterService: ClusterService
  ) {}

  @Effect()
  execute$: Observable<Action> = this.actions$.pipe(
    ofType(
      executionTriggered.type,
    ),
    switchMap(({ sampleData, chainConfig }) => {
      return this.liveViewService.execute(sampleData, chainConfig, this.clusterService.getCurrentCluster()).pipe(
        map(liveViewResult => liveViewRefreshedSuccessfully({ liveViewResult })),
        catchError(( error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(liveViewRefreshFailed({ error }));
        })
      );
    })
  );

  @Effect({ dispatch: false})
  persistingSampleData$ = this.actions$.pipe(
    ofType(
      sampleDataInputChanged.type,
    ),
    tap(({ sampleData }) => {
      localStorage.setItem(LiveViewConsts.SAMPLE_DATA_STORAGE_KEY, JSON.stringify(sampleData));
    })
  );

  @Effect({ dispatch: false})
  persistingOnOffToggle$ = this.actions$.pipe(
    ofType(
      onOffToggleChanged.type,
    ),
    tap(({ value }) => {
      localStorage.setItem(LiveViewConsts.FEATURE_TOGGLE_STORAGE_KEY, JSON.stringify(value));
    })
  );

  @Effect()
  restoreSampleDataFromLocalStore: Observable<Action> = this.actions$.pipe(
    ofType(
      liveViewInitialized.type,
    ),
    switchMap(() => {
      const sampleData = JSON.parse(localStorage.getItem(LiveViewConsts.SAMPLE_DATA_STORAGE_KEY));
      return of(sampleDataRestored({ sampleData }));
    })
  );

  @Effect()
  restoreToggleFromLocalStore: Observable<Action> = this.actions$.pipe(
    ofType(
      liveViewInitialized.type,
    ),
    switchMap(() => {
      const value = JSON.parse(localStorage.getItem(LiveViewConsts.FEATURE_TOGGLE_STORAGE_KEY));
      return of(onOffToggleRestored({ value }));
    })
  );
}
