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
import {TestBed, waitForAsync} from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideMockActions } from '@ngrx/effects/testing';
import { NzModalModule, } from 'ng-zorro-antd/modal';
import {  NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of, ReplaySubject, throwError } from 'rxjs';

import { ChainListPageService } from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import { ChainListEffects } from './chain-list-page.effects';
import { ChainModel } from './chain.model';
import {Action} from "@ngrx/store";

describe('ChainListPage: effects', () => {
  let effects: ChainListEffects;
  let actions: ReplaySubject<Action>;
  let service: ChainListPageService;
  let msgService: NzMessageService;

  beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [
          NzModalModule,
          HttpClientTestingModule,
          NoopAnimationsModule,
        ],
        providers: [
          ChainListEffects,
          provideMockActions(() => actions),
          { provide: ChainListPageService, useValue: jasmine.createSpyObj('ChainListPageService', ["createChain", "deleteChain", "getChains"]) },
          NzMessageService
        ]
      });

      effects = TestBed.inject(ChainListEffects);
      service = TestBed.inject(ChainListPageService);
      msgService = TestBed.inject(NzMessageService);
  }));

  it('loadChains should call the service and return with entries when it succeeds', (done) => {
    const expected = [{
      id: 'id1',
      name: 'Chain 1'
    }];

    service.getChains = (): Observable<ChainModel[]> => of(expected);

    const spy = spyOn(service, 'getChains').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.LoadChainsAction());

    effects.loadChains$.subscribe(result => {
      expect(result).toEqual(new fromActions.LoadChainsSuccessAction(expected));
      done();
    });

    expect(spy).toHaveBeenCalled();
  });

  it('loadChains should return with an error when it fails and call ant`s message service', (done) => {
    const msg = 'Uh-oh!';
    const error = new Error(msg);
    service.getChains = () => throwError(error);

    const spy = spyOn(msgService, 'create').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.LoadChainsAction());

    effects.loadChains$.subscribe(result => {
      expect(result).toEqual(new fromActions.LoadChainsFailAction(error));
      done();
    });

    expect(spy).toHaveBeenCalledWith('error', msg);
  });

  it('createChain should call the service and return with entries when it succeeds', (done) => {
    const initialValue = [{
      id: 'id1',
      name: 'Chain 1'
    }];

    service.getChains = (): Observable<ChainModel[]> => of(initialValue);
    service.createChain = (): Observable<ChainModel> => of({id: 'id2', name: 'Chain 2'});

    const spy = spyOn(service, 'createChain').and.callThrough();
    const spyMsgSrv = spyOn(msgService, 'create').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.CreateChainAction({name: 'Chain 2'}));

    effects.createChain$.subscribe(result => {
      expect(result).toEqual(
        new fromActions.CreateChainSuccessAction({id: 'id2', name: 'Chain 2'})
      );
      done();
    });

    expect(spy).toHaveBeenCalledWith({name: 'Chain 2'});
    expect(spyMsgSrv).toHaveBeenCalledWith(
      'success',
      'Chain Chain 2 has been created'
    );
  });

  it('deleteChain should call the service and return with entries when it succeeds', (done) => {
    const deleteChain = {
      id: 'id1',
      name: 'Chain 1'
    };
    const initialValue = [deleteChain, {
      id: 'id2',
      name: 'Chain 2'
    }];

    service.getChains = (): Observable<ChainModel[]> => of(initialValue);
    service.deleteChain = (): Observable<ChainModel[]> => of(initialValue);

    const spy = spyOn(service, 'deleteChain').and.callThrough();
    const spyMsgSrv = spyOn(msgService, 'create').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.DeleteChainAction(deleteChain.id, deleteChain.name));

    effects.deleteChain$.subscribe(result => {
      expect(result).toEqual(
        new fromActions.DeleteChainSuccessAction(deleteChain.id)
      );
      done();
    });

    expect(spy).toHaveBeenCalledWith(deleteChain.id);
    expect(spyMsgSrv).toHaveBeenCalledWith(
      'success',
      `Chain "${deleteChain.name}" deleted Successfully`
    );
  });

  it('deleteChain should return with an error when it fails', (done) => {
    const initialValue = [{
      id: 'id1',
      name: 'Chain 1'
    }, {
      id: 'id2',
      name: 'Chain 2'
    }];
    const msg = 'Uh-oh!';
    const error = new Error(msg);

    service.getChains = (): Observable<ChainModel[]> => of(initialValue);
    service.deleteChain = () => throwError(error);

    const spy = spyOn(service, 'deleteChain').and.callThrough();
    const spyMsgSrv = spyOn(msgService, 'create').and.callThrough();
    actions = new ReplaySubject(1);
    actions.next(new fromActions.DeleteChainAction('id1', 'Chain 1'));

    effects.deleteChain$.subscribe(result => {
      expect(result).toEqual(
        new fromActions.DeleteChainFailAction(error)
      );
      done();
    });

    expect(spy).toHaveBeenCalledWith('id1');
    expect(spyMsgSrv).toHaveBeenCalledWith(
      'error',
      'Uh-oh!'
    );
  });

  it('hideCreateModal should return with the success action', (done) => {
    actions = new ReplaySubject(1);
    actions.next(new fromActions.CreateChainSuccessAction(null));

    effects.hideCreateModal$.subscribe(result => {
      expect(result).toEqual(new fromActions.HideCreateModalAction());
      done();
    });
  });

  it('hideCreateModal should return with the fail action', (done) => {
    actions = new ReplaySubject(1);
    actions.next(new fromActions.CreateChainFailAction(null));

    effects.hideCreateModal$.subscribe(result => {
      expect(result).toEqual(new fromActions.HideCreateModalAction());
      done();
    });
  });

  it('hideDeleteModal should return with the success action', (done) => {
    actions = new ReplaySubject(1);
    actions.next(new fromActions.DeleteChainSuccessAction("id1"));

    effects.hideDeleteModal$.subscribe(result => {
      expect(result).toEqual(new fromActions.HideDeleteModalAction());
      done();
    });
  });

  it('hideDeleteModal should return with the error action', (done) => {
    actions = new ReplaySubject(1);
    actions.next(new fromActions.DeleteChainFailAction(null));

    effects.hideDeleteModal$.subscribe(result => {
      expect(result).toEqual(new fromActions.HideDeleteModalAction());
      done();
    });
  });

  it('showDeleteModal should return with an action', (done) => {
    actions = new ReplaySubject(1);
    actions.next(new fromActions.SelectDeleteChainAction("id1"));

    effects.showDeleteModal$.subscribe(result => {
      expect(result).toEqual(new fromActions.ShowDeleteModalAction());
      done();
    });
  });

});
