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
import {Actions, createEffect, ofType} from '@ngrx/effects';
import {Action, Store} from '@ngrx/store';
import {NzMessageService} from 'ng-zorro-antd/message';
import {Observable, of} from 'rxjs';
import {catchError, map, switchMap, withLatestFrom} from 'rxjs/operators';

import {ChainListPageService} from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import {ChainModel} from './chain.model';
import {PipelineService} from "../services/pipeline.service";
import {ChainListPageState, getCurrentPipeline} from "./chain-list-page.reducers";

@Injectable()
export class ChainListEffects {

  constructor(
    private actions$: Actions,
    private store$: Store<ChainListPageState>,
    private messageService: NzMessageService,
    private chainListService: ChainListPageService,
    private pipelineService: PipelineService
  ) {
  }

  loadChains$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.LOAD_CHAINS, fromActions.LOAD_PIPELINES_SUCCESS),
    withLatestFrom(this.store$.select(getCurrentPipeline)),
    switchMap(([action, currentPipeline]) => {
        return this.chainListService.getChains(currentPipeline)
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
    withLatestFrom(this.store$.select(getCurrentPipeline)),
    switchMap(([action, currentPipeline]) => {
      const finalAction = (action as fromActions.CreateChainAction)
      return this.chainListService.createChain(finalAction.newChain, currentPipeline)
        .pipe(
          map((chain: ChainModel) => {
            this.messageService.create('success', 'Chain ' + finalAction.newChain.name + ' has been created');
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

  hideRenamePipelineModal$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(
      fromActions.RENAME_PIPELINE_SUCCESS,
      fromActions.RENAME_PIPELINE_FAIL
    ),
    map(() => new fromActions.HideRenamePipelineModalAction())
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
    withLatestFrom(this.store$.select(getCurrentPipeline)),
    switchMap(([action, currentPipeline]) => {
      const finalAction = action as fromActions.DeleteChainAction
      return this.chainListService.deleteChain(finalAction.chainId, currentPipeline)
        .pipe(
          map(() => {
            this.messageService.create('success', 'Chain ' + finalAction.chainName + ' deleted Successfully');
            return new fromActions.DeleteChainSuccessAction(finalAction.chainId);
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
      return this.pipelineService.getPipelines()
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.LoadPipelinesSuccessAction(pipelines);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.LoadPipelinesFailAction(error));
          })
        );
    })
  ));

  createPipeline$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.CREATE_PIPELINE),
    switchMap((action: fromActions.CreatePipelineAction) => {
      return this.pipelineService.createPipeline(action.pipelineName)
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.CreatePipelineSuccessAction(action.pipelineName, pipelines);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.CreatePipelineFailAction(error));
          })
        );
    })
  ));

  renamePipeline$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.RENAME_PIPELINE),
    switchMap((action: fromActions.RenamePipelineAction) => {
      return this.pipelineService.renamePipeline(action.pipelineName, action.newPipelineName)
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.RenamePipelineSuccessAction(action.newPipelineName, pipelines);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.RenamePipelineFailAction(error));
          })
        );
    })
  ));

  deletePipeline$: Observable<Action> = createEffect(() => this.actions$.pipe(
    ofType(fromActions.DELETE_PIPELINE),
    switchMap((action: fromActions.DeletePipelineAction) => {
      return this.pipelineService.deletePipeline(action.pipelineName)
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.DeletePipelineSuccessAction(pipelines);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.DeletePipelineFailAction(error));
          })
        );
    })
  ));

  triggerPipelineChanged$: Observable<Action> = createEffect(() => this.actions$.pipe(
      ofType(fromActions.RENAME_PIPELINE_SUCCESS, fromActions.DELETE_PIPELINE_SUCCESS),
      switchMap((action: fromActions.RenamePipelineSuccessAction) => {
        return of(new fromActions.PipelineChangedAction(action.newPipelineName))
      })
  ));

  pipelineChanged$: Observable<Action> = createEffect(() => this.actions$.pipe(
      ofType(fromActions.PIPELINE_CHANGED),
      switchMap((action: fromActions.PipelineChangedAction) => {
        return of(new fromActions.LoadChainsAction())
      })
  ));
}
