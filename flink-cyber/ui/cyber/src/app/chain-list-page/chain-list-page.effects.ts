import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { ChainListPageService } from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import { ChainModel } from './chain.model';

@Injectable()
export class ChainListEffects {
  constructor(
    private actions$: Actions,
    private messageService: NzMessageService,
    private chainListService: ChainListPageService
  ) { }

  @Effect()
  loadChains$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.LOAD_CHAINS),
    switchMap((action: fromActions.LoadChainsAction) => {
      return this.chainListService.getChains()
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
  );

  @Effect()
  createChain$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.CREATE_CHAIN),
    switchMap((action: fromActions.CreateChainAction) => {
      return this.chainListService.createChain(action.newChain)
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
  );

  @Effect()
  deleteChain$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.DELETE_CHAIN),
    switchMap((action: fromActions.DeleteChainAction) => {
      return this.chainListService.deleteChain(action.chainId)
        .pipe(
          map(() => {
            this.messageService.create('success', action.chainId + ' deleted Successfully');
            return new fromActions.DeleteChainSuccessAction(action.chainId);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.DeleteChainFailAction(error));
          })
        );
    })
  );
}
