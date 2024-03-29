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
import {Action} from '@ngrx/store';
import {NzMessageService} from 'ng-zorro-antd/message';
import {Observable, of} from 'rxjs';
import {catchError, map, switchMap} from 'rxjs/operators';

import {AddParserPageService} from '../services/chain-add-parser-page.service';

import * as fromActions from './chain-add-parser-page.actions';

@Injectable()
export class AddParserEffects {
  getParserTypes$: Observable<Action> = createEffect(() => this._actions$.pipe(
    ofType(fromActions.GET_PARSER_TYPES),
    switchMap(() => {
      return this._addParserService.getParserTypes()
        .pipe(
          map((parserTypes: { id: string, name: string }[]) => {
            return new fromActions.GetParserTypesSuccessAction(parserTypes);
          }),
          catchError((error: { message: string }) => {
            this._messageService.create('error', error.message);
            return of(new fromActions.GetParserTypesFailAction({message: error.message}));
          })
        );
    })
  ));
  constructor(
    private _actions$: Actions,
    private _messageService: NzMessageService,
    private _addParserService: AddParserPageService,
  ) {
  }
}
