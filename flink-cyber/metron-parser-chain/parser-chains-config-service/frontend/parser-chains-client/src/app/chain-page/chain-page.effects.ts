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
import {Actions, Effect, ofType} from '@ngrx/effects';
import {Action, Store} from '@ngrx/store';
import {NzMessageService} from 'ng-zorro-antd/message';
import {Observable, of} from 'rxjs';
import {catchError, map, switchMap, withLatestFrom} from 'rxjs/operators';

import {ChainPageService} from '../services/chain-page.service';
import * as fromActions from './chain-page.actions';
import {ChainDetailsModel} from './chain-page.models';
import {getChainPageState} from './chain-page.reducers';
import {denormalizeParserConfig, normalizeParserConfig} from './chain-page.utils';
import {CustomFormConfig} from './components/custom-form/custom-form.component';

@Injectable()
export class ChainPageEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<any>,
    private messageService: NzMessageService,
    private chainPageService: ChainPageService
  ) {}

  @Effect()
  loadChainDetails$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.LOAD_CHAIN_DETAILS),
    switchMap((action: fromActions.LoadChainDetailsAction) => {
      return this.chainPageService.getChain(action.payload.id).pipe(
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
          this.messageService.create('error', error.message);
          return of(new fromActions.LoadChainDetailsFailAction(error));
        })
      );
    })
  );

  @Effect()
  saveParserConfig$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.SAVE_PARSER_CONFIG),
    withLatestFrom(this.store$.select(getChainPageState)),
    switchMap(([action, state]) => {
      const chainId = (action as fromActions.SaveParserConfigAction).payload.chainId;
      const config = denormalizeParserConfig(state.chains[chainId], state);
      return this.chainPageService.saveParserConfig(chainId, config).pipe(
        map(() => {
          return new fromActions.SaveParserConfigSuccessAction();
        }),
        catchError((error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(new fromActions.SaveParserConfigFailAction(error));
        })
      );
    })
  );

  @Effect()
  getFormConfig$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.GET_FORM_CONFIG),
    switchMap((action: fromActions.GetFormConfigAction) => {
      return this.chainPageService.getFormConfig(action.payload.type).pipe(
        map((formConfig: CustomFormConfig[]) => {
          return new fromActions.GetFormConfigSuccessAction({
            parserType: action.payload.type,
            formConfig
          });
        }),
        catchError((error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(new fromActions.GetFormConfigFailAction(error));
        })
      );
    })
  );

  @Effect()
  getFormConfigs$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.GET_FORM_CONFIGS),
    switchMap((action: fromActions.GetFormConfigsAction) => {
      return this.chainPageService.getFormConfigs().pipe(
        map((formConfigs: { [key: string]: CustomFormConfig[] }) => {
          return new fromActions.GetFormConfigsSuccessAction({
            formConfigs
          });
        }),
        catchError((error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(new fromActions.GetFormConfigsFailAction(error));
        })
      );
    })
  );

  @Effect()
  getIndexMappings$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.GET_INDEX_MAPPINGS),
    switchMap((action: fromActions.GetIndexMappingsAction) => {
      return this.chainPageService.getIndexMappings(action.payload).pipe(
        map((response:{path:string, result:Map<string, object>}) => {
          if (response){
            return new fromActions.GetIndexMappingsSuccessAction({
              path: response.path, result: response.result
            });
          } else {
            return new fromActions.GetIndexMappingsSuccessAction({
              path: '', result: new Map<string, object>()
            });
          }
          }),
        catchError((error: { message: string }) => {
          this.messageService.create('error', error.message);
          return of(new fromActions.GetIndexMappingsFailAction(error));
        })
      );
    })
  );
}
