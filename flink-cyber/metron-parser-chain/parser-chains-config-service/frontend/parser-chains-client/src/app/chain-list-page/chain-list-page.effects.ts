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
import {ChainListPageState, getSelectedPipeline} from "./chain-list-page.reducers";

@Injectable()
export class ChainListEffects {

  loadChains$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.LOAD_CHAINS, fromActions.LOAD_PIPELINES_SUCCESS),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([, selectedPipeline]) => {
        return this._chainListService.getChains(selectedPipeline)
        .pipe(
          map((chains: ChainModel[]) => {
            return new fromActions.LoadChainsSuccessAction(chains);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.LoadChainsFailAction(error));
          })
        );
    })
  ));

  createChain$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.CREATE_CHAIN),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([action, selectedPipeline]) => {
      const finalAction = (action as fromActions.CreateChainAction)
      return this._chainListService.createChain(finalAction.newChain, selectedPipeline)
        .pipe(
          map((chain: ChainModel) => {
            this._messageService.create('success', 'Chain ' + finalAction.newChain.name + ' has been created');
            return new fromActions.CreateChainSuccessAction(chain);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.CreateChainFailAction(error));
          })
        );
    })
  ));

  hideCreateModal$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(
      fromActions.CREATE_CHAIN_SUCCESS,
      fromActions.CREATE_CHAIN_FAIL
    ),
    map(() => new fromActions.HideCreateModalAction())
  ))

  hideRenamePipelineModal$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(
      fromActions.RENAME_PIPELINE_SUCCESS,
      fromActions.RENAME_PIPELINE_FAIL
    ),
    map(() => new fromActions.HideRenamePipelineModalAction())
  ))
  hideDeleteModal$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(
      fromActions.DELETE_CHAIN_SUCCESS,
      fromActions.DELETE_CHAIN_FAIL
    ),
    map(() => new fromActions.HideDeleteModalAction())
  ))

  showDeleteModal$: Observable<Action> = createEffect(() => this._actions$.pipe(
      ofType(fromActions.DELETE_CHAIN_SELECT),
      map(() => new fromActions.ShowDeleteModalAction())
    ),
  )

  deleteChain$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.DELETE_CHAIN),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([action, selectedPipeline]) => {
      const finalAction = action as fromActions.DeleteChainAction
      return this._chainListService.deleteChain(finalAction.chainId, selectedPipeline)
        .pipe(
          map(() => {
            this._messageService.create('success', 'Chain "' + finalAction.chainName + '" deleted Successfully');
            return new fromActions.DeleteChainSuccessAction(finalAction.chainId);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.DeleteChainFailAction(error));
          })
        );
    })
  ));

  loadPipelines$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.LOAD_PIPELINES),
    switchMap((action: fromActions.LoadPipelinesAction) => {
      return this._pipelineService.getPipelines()
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.LoadPipelinesSuccessAction(pipelines);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.LoadPipelinesFailAction(error));
          })
        );
    })
  ));

  createPipeline$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.CREATE_PIPELINE),
    switchMap((action: fromActions.CreatePipelineAction) => {
      return this._pipelineService.createPipeline(action.pipelineName)
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.CreatePipelineSuccessAction(action.pipelineName, pipelines);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.CreatePipelineFailAction(error));
          })
        );
    })
  ));

  renameSelectedPipeline$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.RENAME_SELECTED_PIPELINE),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([action, selectedPipeline]) => {
      const finalAction = action as fromActions.RenameSelectedPipelineAction;
      return this._pipelineService.renamePipeline(selectedPipeline, finalAction.newPipelineName)
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.RenamePipelineSuccessAction(finalAction.newPipelineName, pipelines);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.RenamePipelineFailAction(error));
          })
        );
    })
  ));

  deleteSelectedPipeline$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.DELETE_SELECTED_PIPELINE),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([action, selectedPipeline]) => {
      return this._pipelineService.deletePipeline(selectedPipeline)
        .pipe(
          map((pipelines: string[]) => {
            return new fromActions.DeletePipelineSuccessAction(pipelines);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.DeletePipelineFailAction(error));
          })
        );
    })
  ));

  triggerPipelineChanged$: Observable<Action> = createEffect(() => this._actions$.pipe(
      ofType(fromActions.RENAME_PIPELINE_SUCCESS, fromActions.DELETE_PIPELINE_SUCCESS),
      switchMap((action: fromActions.RenamePipelineSuccessAction) => {
        return of(new fromActions.PipelineChangedAction(action.newPipelineName))
      })
  ));

  pipelineChanged$: Observable<Action> = createEffect(() => this._actions$.pipe(
      ofType(fromActions.PIPELINE_CHANGED),
      switchMap((action: fromActions.PipelineChangedAction) => {
        return of(new fromActions.LoadChainsAction())
      })
  ));

  constructor(
    private _actions$: Actions,
    private _store$: Store<ChainListPageState>,
    private _messageService: NzMessageService,
    private _chainListService: ChainListPageService,
    private _pipelineService: PipelineService
  ) {
  }
}
