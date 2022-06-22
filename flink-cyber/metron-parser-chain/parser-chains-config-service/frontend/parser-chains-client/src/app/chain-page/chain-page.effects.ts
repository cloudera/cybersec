import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { NzMessageService } from 'ng-zorro-antd';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';

import { ChainPageService } from './../services/chain-page.service';
import * as fromActions from './chain-page.actions';
import { ChainDetailsModel } from './chain-page.models';
import { getChainPageState } from './chain-page.reducers';
import { denormalizeParserConfig, normalizeParserConfig } from './chain-page.utils';
import { CustomFormConfig } from './components/custom-form/custom-form.component';

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
}
