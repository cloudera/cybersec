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
import {TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {provideMockActions} from '@ngrx/effects/testing';
import {provideMockStore} from '@ngrx/store/testing';
import {NzModalModule} from 'ng-zorro-antd/modal';
import {of, ReplaySubject} from 'rxjs';

import {ChainPageService} from '../services/chain-page.service';

import * as fromActions from './chain-page.actions';
import {ChainPageEffects} from './chain-page.effects';
import {NzMessageService} from "ng-zorro-antd/message";
import {ParserDescriptor} from "./chain-page.reducers";
import {Action} from "@ngrx/store";

const selectedPipeline = 'foo-pipeline';
const initialChains = {
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
};

const initialParsers = {
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
};

const initialRoutes = {
  123: {
    id: '123',
    name: 'some route',
    subchain: '456'
  }
};

const chainListPageInitialState = {
  items: [],
  createModalVisible: false,
  deleteModalVisible: false,
  deleteItem: null,
  loading: false,
  error: '',
  pipelines: null,
  pipelineRenameModalVisible: false,
  selectedPipeline
};

describe('chain parser page: effects', () => {
  let actions: ReplaySubject<Action>;
  let effects: ChainPageEffects;
  let service: jasmine.SpyObj<ChainPageService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        RouterTestingModule,
        HttpClientTestingModule,
      ],
      providers: [
        ChainPageEffects,
        provideMockActions(() => actions),
        provideMockStore({
          initialState: {
            'chain-page': {
              chains: initialChains,
              parsers: initialParsers,
              routes: initialRoutes,
              selectedPipeline
            },
            'chain-list-page': chainListPageInitialState
          },
          selectors: []
        }),
        {
          provide: ChainPageService,
          useValue: jasmine.createSpyObj('ChainPageService', ['getChain', 'saveParserConfig', 'getFormConfig', 'getFormConfigs', 'getIndexMappings'])
        },
        NzMessageService
      ]
    });
    effects = TestBed.inject(ChainPageEffects);
    service = TestBed.inject(ChainPageService) as jasmine.SpyObj<ChainPageService>;
  });

  it('load should receive the parser config and normalize it', (done) => {
    const chain = {
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
    };
    const normalizedChain = {
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
    };
    service.getChain.and.returnValue(of(chain));


    actions = new ReplaySubject(1);
    actions.next(new fromActions.LoadChainDetailsAction({
      id: '123',
      currentPipeline: selectedPipeline    }));

    effects.loadChainDetails$.subscribe(result => {
      expect(result).toEqual(new fromActions.LoadChainDetailsSuccessAction(normalizedChain));
      done();
    });
    expect(service.getChain).toHaveBeenCalledWith('123', selectedPipeline);
  });

  it('save should send the denormalized parser config', (done) => {
    service.saveParserConfig.and.returnValue(of(void 0));

    actions = new ReplaySubject(1);
    actions.next(new fromActions.SaveParserConfigAction({
      chainId: '123'
    }));

    effects.saveParserConfig$.subscribe(result => {
      expect(result).toEqual(new fromActions.SaveParserConfigSuccessAction());
      done();
    });

    expect(service.saveParserConfig).toHaveBeenCalledWith('123', {
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
    }, selectedPipeline);
  });

  it('getFormConfig should return the form config', (done) => {
    const descriptor: { [key: string]: ParserDescriptor } = {
      foo: {
        id: 'foo-descriptor',
        name: 'foo-descriptor',
        schemaItems: [{
          type: 'bazz',
          id: '1',
          name: 'foo-schema-item'
        }]
      }
    };
    service.getFormConfig.and.returnValue(of(descriptor.foo));

    actions = new ReplaySubject(1);
    actions.next(new fromActions.GetFormConfigAction({
      type: 'foo'
    }));

    effects.getFormConfig$.subscribe(result => {
      expect(result).toEqual(new fromActions.GetFormConfigSuccessAction({
        parserType: 'foo',
        formConfig: descriptor.foo
      }));
      done();
    });

    expect(service.getFormConfig).toHaveBeenCalledWith('foo');
  });

  it('getFormConfigs should return the form configs', (done) => {
    const descriptor: { [key: string]: ParserDescriptor } = {
      foo: {
        id: 'foo-descriptor',
        name: 'foo-descriptor',
        schemaItems: [{
          type: 'bazz',
          id: '1',
          name: 'foo-schema-item'
        }]
      }
    };
    service.getFormConfigs.and.returnValue(of(descriptor));

    actions = new ReplaySubject(1);
    actions.next(new fromActions.GetFormConfigsAction());

    effects.getFormConfigs$.subscribe(result => {
      expect(result).toEqual(new fromActions.GetFormConfigsSuccessAction({
        formConfigs: descriptor
      }));
      done();
    });

    expect(service.getFormConfigs).toHaveBeenCalledWith();
  });
});
