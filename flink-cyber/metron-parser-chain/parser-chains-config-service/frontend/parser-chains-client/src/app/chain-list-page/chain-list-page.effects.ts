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

import {Injectable} from '@angular/core';
import {Actions, createEffect, Effect, ofType} from '@ngrx/effects';
import {Action, select, Store} from '@ngrx/store';
import {NzMessageService} from 'ng-zorro-antd/message';
import {Observable, of} from 'rxjs';
import {catchError, map, switchMap, take} from 'rxjs/operators';

import {ChainListPageService} from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import {ChainModel, PipelineModel} from './chain.model';
import {PipelineService} from "../services/pipeline.service";
import {ChainListPageState, getChains, getCurrentPipeline} from "./chain-list-page.reducers";
import {ClusterService} from "../services/cluster.service";

@Injectable()
export class ChainListEffects {

  constructor(
    private actions$: Actions,
    private messageService: NzMessageService,
    private chainListService: ChainListPageService,
    private pipelineService: PipelineService,
    private clusterService: ClusterService
  ) {
  }

  loadChains$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.LOAD_CHAINS),
    switchMap((action: fromActions.LoadChainsAction) => {
        return this.chainListService.getChains(this.pipelineService.getCurrentPipeline(), this.clusterService.getCurrentCluster())
        .pipe(
          map((chains: ChainModel[]) => {
            return new fromActions.LoadChainsSuccessAction(chains);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.LoadChainsFailAction(error));
          })
        );
    })
  ));

    createChain$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.CREATE_CHAIN),
    switchMap((action: fromActions.CreateChainAction) => {
      return this.chainListService.createChain(action.newChain, this.pipelineService.getCurrentPipeline(), this.clusterService.getCurrentCluster())
        .pipe(
          map((chain: ChainModel) => {
            this.messageService.create('success', 'Chain ' + action.newChain.name + ' has been created');
            return new fromActions.CreateChainSuccessAction(chain);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.CreateChainFailAction(error));
          })
        );
    })
  ));

  hideCreateModal$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(
      fromActions.CREATE_CHAIN_SUCCESS,
      fromActions.CREATE_CHAIN_FAIL
    ),
    map(() => new fromActions.HideCreateModalAction())
  ))
  hideDeleteModal$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(
      fromActions.DELETE_CHAIN_SUCCESS,
      fromActions.DELETE_CHAIN_FAIL
    ),
    map(() => new fromActions.HideDeleteModalAction())
  ))

  showDeleteModal$: Observable<Action> = createEffect(() => this.actions$.pipe(
      ofType(fromActions.DELETE_CHAIN_SELECT),
      map(() => new fromActions.ShowDeleteModalAction())
    ),
  )

  deleteChain$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.DELETE_CHAIN),
    switchMap((action: fromActions.DeleteChainAction) => {
      return this.chainListService.deleteChain(action.chainId, this.pipelineService.getCurrentPipeline(), this.clusterService.getCurrentCluster())
        .pipe(
          map(() => {
            this.messageService.create('success', 'Chain ' + action.chainName + ' deleted Successfully');
            return new fromActions.DeleteChainSuccessAction(action.chainId);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.DeleteChainFailAction(error));
          })
        );
    })
  ));

  loadPipelines$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.LOAD_PIPELINES),
    switchMap((action: fromActions.LoadPipelinesAction) => {
      return this.pipelineService.getPipelines(this.clusterService.getCurrentCluster())
        .pipe(
          map((pipelines: PipelineModel[]) => {
            return new fromActions.LoadPipelinesSuccessAction(pipelines);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.LoadPipelinesFailAction(error));
          })
        );
    })
  ));
}
