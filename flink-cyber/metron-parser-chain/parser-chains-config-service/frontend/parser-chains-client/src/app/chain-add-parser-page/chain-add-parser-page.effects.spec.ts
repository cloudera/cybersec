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

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { Observable, of, ReplaySubject } from 'rxjs';

import { AddParserPageService } from 'services/chain-add-parser-page.service';

import * as fromActions from './chain-add-parser-page.actions';
import { AddParserEffects } from './chain-add-parser-page.effects';

export class MockService {}

describe('chain add parser page: effects', () => {
  let effects: AddParserEffects;
  let actions: ReplaySubject<any>;
  let service: AddParserPageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        provideMockActions,
        NzModalModule,
        HttpClientTestingModule
      ],
      providers: [
        AddParserEffects,
        provideMockActions(() => actions),
        { provide: AddParserPageService, useClass: MockService },
      ]
    });

    effects = TestBed.get(AddParserEffects);
    service = TestBed.get(AddParserPageService);
  });

  it('get parser types should call the service and return with the parser types when it succeeds', () => {
    const expected = [{
      id: '1',
      name: 'foo'
    }, {
      id: '2',
      name: 'bar'
    }];

    service.getParserTypes = (): Observable<any> => of(expected);

    const spy = spyOn(service, 'getParserTypes').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.GetParserTypesAction());

    effects.getParserTypes$.subscribe(result => {
      expect(result).toEqual(new fromActions.GetParserTypesSuccessAction(expected));
    });

    expect(spy).toHaveBeenCalledWith();
  });
});
