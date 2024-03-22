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

import {HttpClientTestingModule} from '@angular/common/http/testing';
import {TestBed, waitForAsync} from '@angular/core/testing';
import {provideMockActions} from '@ngrx/effects/testing';
import {NzModalModule} from 'ng-zorro-antd/modal';
import {of, ReplaySubject, throwError} from 'rxjs';

import {AddParserPageService} from '../services/chain-add-parser-page.service';

import * as fromActions from './chain-add-parser-page.actions';
import {AddParserEffects} from './chain-add-parser-page.effects';
import {NzMessageService} from "ng-zorro-antd/message";
import {GetParserTypesFailAction} from "./chain-add-parser-page.actions";

describe('chain add parser page: effects', () => {
  let effects: AddParserEffects;
  let actions: ReplaySubject<any>;
  let service: jasmine.SpyObj<AddParserPageService>;
  let messageService: jasmine.SpyObj<NzMessageService>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        HttpClientTestingModule
      ],
      providers: [
        AddParserEffects,
        provideMockActions(() => actions),
        {provide: AddParserPageService, useValue: jasmine.createSpyObj('AddParserPageService', ['getParserTypes', 'getParser', 'add'])},
        {provide: NzMessageService, useValue: jasmine.createSpyObj('NzMessageService', ['create'])}
      ]
    });
    effects = TestBed.inject(AddParserEffects);
    service = TestBed.inject(AddParserPageService) as jasmine.SpyObj<AddParserPageService>;
    messageService = TestBed.inject(NzMessageService) as jasmine.SpyObj<NzMessageService>;
  }));

  it('get parser types should call the service and return with the parser types when it succeeds', (done) => {
    const expected = [{
      id: '1',
      name: 'foo'
    }, {
      id: '2',
      name: 'bar'
    }];
    service.getParserTypes = jasmine.createSpy('getParserTypes').and.returnValue(of(expected));
    actions = new ReplaySubject(1);
    actions.next(new fromActions.GetParserTypesAction());

    effects.getParserTypes$.subscribe(result => {
      expect(result).toEqual(new fromActions.GetParserTypesSuccessAction(expected));
      done();
    });

    expect(service.getParserTypes).toHaveBeenCalled();
  });

  it('get parser types should call the service and return with an error when it fails', (done) => {
    const errorMessage = "Something bad happens";
    service.getParserTypes = jasmine.createSpy('getParserTypes').and.returnValue(throwError(new Error(errorMessage)));
    messageService.create = jasmine.createSpy('create').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.GetParserTypesAction());

    effects.getParserTypes$.subscribe(action => {
      expect(action).toEqual(new GetParserTypesFailAction({message: errorMessage}));
      done();
    });

    expect(messageService.create).toHaveBeenCalledWith('error', errorMessage);
    expect(service.getParserTypes).toHaveBeenCalled();
  });
});
