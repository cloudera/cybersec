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

import {ChainPageService} from '../services/chain-page.service';
import * as fromActions from './chain-page.actions';
import {ChainDetailsModel} from './chain-page.models';
import {getChainPageState, ParserDescriptor} from './chain-page.reducers';
import {denormalizeParserConfig, normalizeParserConfig} from './chain-page.utils';
import {getSelectedPipeline} from "../chain-list-page/chain-list-page.reducers";

@Injectable()
export class ChainPageEffects {
  loadChainDetails$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.LOAD_CHAIN_DETAILS),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([action, selectedPipeline]) => {
        const finalAction = action as fromActions.LoadChainDetailsAction;
        return this._chainPageService.getChain(finalAction.payload.id, selectedPipeline).pipe(
        map((chain: ChainDetailsModel) => {
          const normalizedParserConfig = normalizeParserConfig(chain);
          return new fromActions.LoadChainDetailsSuccessAction(
            {
              ...normalizedParserConfig,
              chainId: chain.id
            }
          );
        }),
        catchError((error: { message: string }) => {
          this._messageService.create('error', error.message);
          return of(new fromActions.LoadChainDetailsFailAction(error));
        })
      );
    })
  ));


  saveParserConfig$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.SAVE_PARSER_CONFIG),
    withLatestFrom(this._store$.select(getChainPageState)),
    withLatestFrom(this._store$.select(getSelectedPipeline)),
    switchMap(([[action, state], selectedPipeline]) => {
      const finalAction = action as fromActions.SaveParserConfigAction;
      const chainId = finalAction.payload.chainId;
      const config = denormalizeParserConfig(state.chains[chainId], state);
      return this._chainPageService.saveParserConfig(chainId, config, selectedPipeline).pipe(
        map(() => {
          return new fromActions.SaveParserConfigSuccessAction();
        }),
        catchError((error: { message: string }) => {
          this._messageService.create('error', error.message);
          return of(new fromActions.SaveParserConfigFailAction(error));
        })
      );
    })
  ));

  getFormConfig$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.GET_FORM_CONFIG),
    switchMap((action: fromActions.GetFormConfigAction) => {
      return this._chainPageService.getFormConfig(action.payload.type).pipe(
        map((descriptor: ParserDescriptor) => {
          return new fromActions.GetFormConfigSuccessAction({
            parserType: action.payload.type,
            formConfig: descriptor
          });
        }),
        catchError((error: { message: string }) => {
          this._messageService.create('error', error.message);
          return of(new fromActions.GetFormConfigFailAction(error));
        })
      );
    })
  ));

  getFormConfigs$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.GET_FORM_CONFIGS),
    switchMap((action: fromActions.GetFormConfigsAction) => {
      return this._chainPageService.getFormConfigs().pipe(
        map((formConfigs: { [key: string]: ParserDescriptor }) => {
          return new fromActions.GetFormConfigsSuccessAction({
            formConfigs
          });
        }),
        catchError((error: { message: string }) => {
          this._messageService.create('error', error.message);
          return of(new fromActions.GetFormConfigsFailAction(error));
        })
      );
    })
  ));

  constructor(
    private _actions$: Actions,
    private _store$: Store<any>,
    private _messageService: NzMessageService,
    private _chainPageService: ChainPageService
  ) {
  }
}
