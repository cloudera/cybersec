import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { IconDefinition } from '@ant-design/icons-angular';
import { PlusCircleOutline } from '@ant-design/icons-angular/icons';
import { provideMockActions } from '@ngrx/effects/testing';
import { NzModalModule, } from 'ng-zorro-antd/modal';
import { NZ_ICONS } from 'ng-zorro-antd/icon';
import {  NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of, ReplaySubject, throwError } from 'rxjs';

import { ChainListPageService } from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import { ChainListEffects } from './chain-list-page.effects';
import { ChainModel } from './chain.model';
export class MockChainListPageService {
  deleteChain(id: string) {
    return of({});
  }
  createChain(name: string) {
    return of({});
  }
}
const icons: IconDefinition[] = [PlusCircleOutline];

describe('ChainListPage: effects', () => {
  let effects: ChainListEffects;
  let actions: ReplaySubject<any>;
  let service: ChainListPageService;
  let msgService: NzMessageService;

  beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [
          NzModalModule,
          HttpClientTestingModule,
          NoopAnimationsModule
        ],
        providers: [
          ChainListEffects,
          provideMockActions(() => actions),
          { provide: ChainListPageService, useClass: MockChainListPageService },
          { provide: NZ_ICONS, useValue: icons }
        ]
      });

      effects = TestBed.get(ChainListEffects);
      service = TestBed.get(ChainListPageService);
      msgService = TestBed.get(NzMessageService);
  });

  it('loadChains should call the service and return with entries when it succeeds', () => {
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
    });

    expect(spy).toHaveBeenCalled();
  });

  it('loadChains should return with an error when it fails and call ant`s message service', () => {
    const msg = 'Uh-oh!';
    const error = new Error(msg);
    service.getChains = () => throwError(error);

    const spy = spyOn(msgService, 'create').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.LoadChainsAction());

    effects.loadChains$.subscribe(result => {
      expect(result).toEqual(new fromActions.LoadChainsFailAction(error));
    });

    expect(spy).toHaveBeenCalledWith('error', msg);
  });

  it('createChain should call the service and return with entries when it succeeds', () => {
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
    });

    expect(spy).toHaveBeenCalledWith({name: 'Chain 2'});
    expect(spyMsgSrv).toHaveBeenCalledWith(
      'success',
      'Chain Chain 2 has been created'
    );
  });

  it('deleteChain should call the service and return with entries when it succeeds', () => {
    const initialValue = [{
      id: 'id1',
      name: 'Chain 1'
    }, {
      id: 'id2',
      name: 'Chain 2'
    }];

    service.getChains = (): Observable<ChainModel[]> => of(initialValue);
    service.deleteChain = (): Observable<ChainModel[]> => of(initialValue);

    const spy = spyOn(service, 'deleteChain').and.callThrough();
    const spyMsgSrv = spyOn(msgService, 'create').and.callThrough();

    actions = new ReplaySubject(1);
    actions.next(new fromActions.DeleteChainAction('id1', 'Chain 1'));

    effects.deleteChain$.subscribe(result => {
      expect(result).toEqual(
        new fromActions.DeleteChainSuccessAction('id1')
      );
    });

    expect(spy).toHaveBeenCalledWith('id1');
    expect(spyMsgSrv).toHaveBeenCalledWith(
      'success',
      'id1 deleted Successfully'
    );
  });

  it('deleteChain should return with an error when it fails', () => {
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
    });

    expect(spy).toHaveBeenCalledWith('id1');
    expect(spyMsgSrv).toHaveBeenCalledWith(
      'error',
      'Uh-oh!'
    );
  });
});
