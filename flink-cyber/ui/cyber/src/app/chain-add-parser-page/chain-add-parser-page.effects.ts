import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AddParserPageService } from '../services/chain-add-parser-page.service';

import * as fromActions from './chain-add-parser-page.actions';

@Injectable()
export class AddParserEffects {
  constructor(
    private actions$: Actions,
    private messageService: NzMessageService,
    private addParserService: AddParserPageService,
  ) { }

  @Effect()
  getParserTypes$: Observable<Action> = this.actions$.pipe(
    ofType(fromActions.GET_PARSER_TYPES),
    switchMap(() => {
      return this.addParserService.getParserTypes()
        .pipe(
          map((parserTypes: { id: string, name: string }[]) => {
            return new fromActions.GetParserTypesSuccessAction(parserTypes);
          }),
          catchError((error: { message: string }) => {
            this.messageService.create('error', error.message);
            return of(new fromActions.GetParserTypesFailAction(error));
          })
        );
    })
  );
}
