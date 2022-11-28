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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DeleteFill, EditFill } from '@ant-design/icons-angular/icons';
import { Store, StoreModule } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NZ_ICONS } from 'ng-zorro-antd/icon'

import * as fromActions from '../../chain-page.actions';
import * as fromReducers from '../../chain-page.reducers';

import { RouteComponent } from './route.component';

describe('RouteComponent', () => {
  let component: RouteComponent;
  let fixture: ComponentFixture<RouteComponent>;
  let store: Store<fromReducers.ChainPageState>;
  const initialState = {
    'chain-page': {
      parsers: {},
      routes: {
        123: {
          id: '123',
          name: 'some route',
          subchain: '456',
          default: false,
        }
      },
      chains: {
        456: {
          id: '456',
          name: 'some chain',
          parsers: []
        }
      },
      error: ''
    }
  };
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        StoreModule.forRoot({
          'chain-page': fromReducers.reducer
        })
      ],
      declarations: [ RouteComponent ],
      providers: [
        provideMockStore({ initialState }),
        { provide: NZ_ICONS, useValue: [EditFill, DeleteFill] }
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RouteComponent);
    component = fixture.componentInstance;
    component.routeId = '123';
    component.parser = {
      id: '678',
      name: 'parser',
      type: 'foo'
    };
    store = TestBed.get(Store);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have a route on init', () => {
    expect(component.route).toEqual({
      id: '123',
      name: 'some route',
      subchain: '456',
      default: false,
    });
  });

  it('should have a subchain on init', () => {
    expect(component.subchain).toEqual({
      id: '456',
      name: 'some chain',
      parsers: []
    });
  });

  it('should emit a chain click event', () => {
    const spy = spyOn(component.chainClick, 'emit');
    component.onChainClick(new Event('click'), '123');
    expect(spy).toHaveBeenCalledWith('123');
  });

  it('should unsubscribe properly', () => {
    const getRouteSubSpy = spyOn(component.getRouteSub, 'unsubscribe');
    const getChainSubSpy = spyOn(component.getChainSub, 'unsubscribe');

    component.ngOnDestroy();

    expect(getRouteSubSpy).toHaveBeenCalledWith();
    expect(getChainSubSpy).toHaveBeenCalledWith();
  });

  it('should dispatch remove', () => {
    const spy = spyOn(store, 'dispatch');
    component.onRouteRemoveConfirmed(new Event('click'), {
      id: '123',
      name: 'route',
      subchain: '456',
      default: false,
    });
    expect(spy).toHaveBeenCalledWith(
      new fromActions.RemoveRouteAction({
        routeId: '123',
        chainId: '456',
        parserId: '678'
      })
    );
  });

  it('should dispatch update chain and update route', () => {
    const spy = spyOn(store, 'dispatch');
    component.onMatchingValueBlur(({
      target: {
        value: '  trim me!    '
      }
    } as unknown) as Event, {
      id: '123',
      name: 'route',
      subchain: '456',
      default: false,
    });
    expect(spy).toHaveBeenCalledWith(
      new fromActions.UpdateChainAction({
        chain: {
          id: '456',
          name: 'trim me!'
        }
      })
    );
    expect(spy).toHaveBeenCalledWith(
      new fromActions.UpdateRouteAction({
        chainId: '456',
        parserId: '678',
        route: {
          id: '123',
          matchingValue: 'trim me!'
        }
      })
    );
  });

  it('should not dispatch anything if the value is the same', () => {
    const spy = spyOn(store, 'dispatch');
    component.onMatchingValueBlur(({
      target: {
        value: '  foobar    '
      }
    } as unknown) as Event, {
      id: '123',
      name: 'route',
      default: false,
      subchain: '456',
      matchingValue: 'foobar'
    });
    expect(spy).not.toHaveBeenCalled();
  });

  it('should dispatch set default route action', () => {
    const spy = spyOn(store, 'dispatch');
    component.parser = {
      id: '101112',
      name: 'some parser',
      type: 'foo'
    };
    component.subchain = {
      id: '456',
      name: 'some chain',
      parsers: ['101112']
    };
    component.onDefaultCheckboxChange(new Event('click'), {
      id: '123',
      name: 'some route',
      default: false,
      subchain: '456'
    });
    expect(spy).toHaveBeenCalledWith(
      new fromActions.SetRouteAsDefaultAction({
        chainId: '456',
        parserId: '101112',
        routeId: '123',
      })
    );
  });
});
