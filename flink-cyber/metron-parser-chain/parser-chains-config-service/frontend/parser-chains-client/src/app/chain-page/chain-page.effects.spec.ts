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
import { RouterTestingModule } from '@angular/router/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { StoreModule } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { of, ReplaySubject } from 'rxjs';

import { ChainPageService } from '../services/chain-page.service';

import * as fromActions from './chain-page.actions';
import { ChainPageEffects } from './chain-page.effects';
import * as fromReducers from './chain-page.reducers';

export class MockService {}

describe('chain parser page: effects', () => {
  let actions: ReplaySubject<any>;
  let effects: ChainPageEffects;
  let service: ChainPageService;

  const initialState = {
    'chain-page': {
      chains: {
        123: {
          id: '123',
          name: 'main chain',
          parsers: ['123']
        },
        456: {
          id: '456',
          name: 'some chain',
          parsers: ['456']
        }
      },
      parsers: {
        123: {
          id: '123',
          name: 'some parser',
          type: 'Router',
          routing: {
            routes: ['123']
          }
        },
        456: {
          id: '456',
          name: 'some other parser',
          type: 'grok'
        }
      },
      routes: {
        123: {
          id: '123',
          name: 'some route',
          subchain: '456'
        }
      }
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        RouterTestingModule,
        HttpClientTestingModule,
        StoreModule.forRoot({
          'chain-page': fromReducers.reducer
        })
      ],
      providers: [
        ChainPageEffects,
        provideMockActions(() => actions),
        provideMockStore({ initialState }),
        { provide: ChainPageService, useClass: MockService },
      ]
    });

    effects = TestBed.get(ChainPageEffects);
    service = TestBed.get(ChainPageService);
  });

  it('load should receive the parser config and normalize it', () => {
    service.getChain = () => of({
      id: '123',
      name: 'main chain',
      parsers: [{
        id: '123',
        name: 'some parser',
        type: 'Router',
        routing: {
          routes: [{
            id: '123',
            name: 'some route',
            default: false,
            subchain: {
              id: '456',
              name: 'some chain',
              parsers: [{
                id: '456',
                name: 'some other parser',
                type: 'grok'
              }]
            }
          }]
        }
      }]
    });

    const getChainSpy = spyOn(service, 'getChain').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.LoadChainDetailsAction({
      id: '123'
    }));

    effects.loadChainDetails$.subscribe(result => {
      expect(result).toEqual(new fromActions.LoadChainDetailsSuccessAction({
        chainId: '123',
        chains: {
          123: {
            id: '123',
            name: 'main chain',
            parsers: ['123']
          },
          456: {
            id: '456',
            name: 'some chain',
            parsers: ['456']
          }
        },
        parsers: {
          123: {
            id: '123',
            name: 'some parser',
            type: 'Router',
            routing: {
              routes: ['123']
            }
          },
          456: {
            id: '456',
            name: 'some other parser',
            type: 'grok'
          }
        },
        routes: {
          123: {
            id: '123',
            name: 'some route',
            subchain: '456',
            default: false,
          }
        }
      }));
    });

    expect(getChainSpy).toHaveBeenCalledWith('123');
  });

  it('save should send the denormalized parser config', () => {
    service.saveParserConfig = () => of();

    const saveParserConfigSpy = spyOn(service, 'saveParserConfig').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.SaveParserConfigAction({
      chainId: '123'
    }));

    effects.saveParserConfig$.subscribe(result => {
      expect(result).toEqual(new fromActions.SaveParserConfigSuccessAction());
    });

    expect(saveParserConfigSpy).toHaveBeenCalledWith('123', {
      id: '123',
      name: 'main chain',
      parsers: [{
        id: '123',
        name: 'some parser',
        type: 'Router',
        routing: {
          routes: [{
            id: '123',
            name: 'some route',
            subchain: {
              id: '456',
              name: 'some chain',
              parsers: [{
                id: '456',
                name: 'some other parser',
                type: 'grok'
              }]
            }
          }]
        }
      }]
    });
  });
});
