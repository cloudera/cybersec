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

import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {IconDefinition} from '@ant-design/icons-angular';
import {DeleteFill, PlusOutline, RightSquareFill} from '@ant-design/icons-angular/icons';
import {Store} from '@ngrx/store';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {NzModalModule} from 'ng-zorro-antd/modal';
import {NzIconModule} from 'ng-zorro-antd/icon'
import {of, ReplaySubject} from 'rxjs';

import {ChainListPageService} from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import {ChainListPageComponent} from './chain-list-page.component';
import {ChainModel} from './chain.model';
import {NzMessageService} from "ng-zorro-antd/message";
import {NzCardModule} from "ng-zorro-antd/card";
import {NzTableModule} from "ng-zorro-antd/table";
import {NzDividerModule} from "ng-zorro-antd/divider";
import {CommonModule} from "@angular/common";
import {NzInputModule} from "ng-zorro-antd/input";
import {getCreateModalVisible, getDeleteChain, getDeleteModalVisible} from "./chain-list-page.reducers";
import {NzToolTipModule} from "ng-zorro-antd/tooltip";
import {NzButtonModule} from "ng-zorro-antd/button";
import {NzPopconfirmModule} from "ng-zorro-antd/popconfirm";
import {NzFormModule} from "ng-zorro-antd/form";
import {NzLayoutModule} from "ng-zorro-antd/layout";
import {provideMockActions} from "@ngrx/effects/testing";

const icons: IconDefinition[] = [PlusOutline, DeleteFill, RightSquareFill];

const chains: ChainModel[] = [
  {id: 'id1', name: 'Chain 1'},
  {id: 'id2', name: 'Chain 2'},
  {id: 'id3', name: 'Chain 3'}
];

class FakeChainListPageService {
  getChains() {
    return of(chains);
  }

  deleteChain() {
    return of([]);
  }

  createChain() {
    return of({});
  }
}

describe('ChainListPageComponent', () => {
  let component: ChainListPageComponent;
  let fixture: ComponentFixture<ChainListPageComponent>;
  let actions: ReplaySubject<any>;
  let store: MockStore<{
    'chain-list-page': {
      loading: boolean;
      error: string;
      items: ChainModel[];
      createModalVisible: boolean;
      deleteModalVisible: boolean;
    }
  }>;


  const chainListPageState = {
    loading: false,
    error: '',
    items: chains,
    createModalVisible: false,
    deleteModalVisible: false
  };
  const initialState = {
    'chain-list-page': chainListPageState
  };

  function clickDeleteBtnOnIndex(index: number) {
    fixture.debugElement
      .queryAll(By.css('.chain-delete-btn'))
      [index].nativeElement.click();
  }

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NzInputModule,
        NzTableModule,
        NzCardModule,
        NzDividerModule,
        NzToolTipModule,
        NzButtonModule,
        NzPopconfirmModule,
        NzIconModule.forChild(icons),
        NzFormModule,
        NzLayoutModule,
        RouterTestingModule,
        NoopAnimationsModule,
      ],
      declarations: [ChainListPageComponent],
      providers: [
        provideMockActions(() => actions),
        provideMockStore({
          initialState: initialState,
          selectors: [
            {selector: getDeleteModalVisible, value: false},
            {selector: getDeleteChain, value: null},
            {selector: getCreateModalVisible, value: false}
          ]
        }),
        {
          provide: ChainListPageService,
          useClass: FakeChainListPageService
        },
        NzMessageService
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.inject(Store) as MockStore<any>;
    fixture = TestBed.createComponent(ChainListPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fire action when user click the delete button', () => {
    spyOn(store, 'dispatch').and.callThrough();

    const indexOfSecondDeleteBtn = 1;
    clickDeleteBtnOnIndex(indexOfSecondDeleteBtn);
    fixture.detectChanges();

    const action = new fromActions.SelectDeleteChainAction(chains[indexOfSecondDeleteBtn].id);
    expect(store.dispatch).toHaveBeenCalledWith(action);
  });

  it('should not show any modal when modal', () => {
    let isDeleteVisible: boolean  = null;
    let deleteChain: ChainModel = null;
    let isCreateVisible: boolean = null;
    store.refreshState();
    fixture.detectChanges();
    const modalWindow = document.querySelector('.ant-modal');
    const chainNameField = document.querySelector('[data-qe-id="chain-name"]');

    component.isChainDeleteModalVisible$.subscribe(
      value => isDeleteVisible = value
    );
    component.deleteChainItem$.subscribe(
      item => deleteChain = item
    );
    component.isChainCreateModalVisible$.subscribe(
      value => isCreateVisible = value
    );

    expect(chainNameField).toBeNull();
    expect(modalWindow).toBeNull();
    expect(isDeleteVisible).toBe(false);
    expect(isCreateVisible).toBe(false);
    expect(deleteChain).toBeNull();
  });

  it('should not show the delete modal when modal is visible but chain is not selected', () => {
    let isVisible: boolean  = null;
    let deleteChain: ChainModel = null;
    component.isChainDeleteModalVisible$.subscribe(value => isVisible = value);
    component.deleteChainItem$.subscribe(value => deleteChain = value);

    store.overrideSelector(getDeleteModalVisible, true);
    store.refreshState();
    fixture.detectChanges();

    const modalWindow = document.querySelector('.ant-modal');

    expect(modalWindow).toBeNull();
    expect(isVisible).toBe(true);
    expect(deleteChain).toBeNull();
  });

  it('should not show the delete modal when modal is not visible but chain is selected', () => {
    const chain = {id: 'id2', name: 'Chain 2'} as ChainModel;
    let isVisible: boolean  = null;
    let deleteChain: ChainModel = null;
    component.isChainDeleteModalVisible$.subscribe(value => isVisible = value);
    component.deleteChainItem$.subscribe(value => deleteChain = value);

    store.overrideSelector(getDeleteChain, chain);
    store.refreshState();
    fixture.detectChanges();

    let modalWindow = document.querySelector('.ant-modal');

    expect(modalWindow).toBeNull();
    expect(isVisible).toBe(false);
    expect(deleteChain).toEqual(chain);
  });

  it('should dispatch an action to delete the chain', () => {
    spyOn(store, 'dispatch').and.callThrough();
    const chain = {id: 'id2', name: 'Chain 2'};
    let isVisible: boolean  = null;
    let deleteChain: ChainModel = null;
    component.isChainDeleteModalVisible$.subscribe(value => isVisible = value);
    component.deleteChainItem$.subscribe(value => deleteChain = value);

    store.overrideSelector(getDeleteModalVisible, true);
    store.overrideSelector(getDeleteChain, chain);
    store.refreshState();
    fixture.detectChanges();

    const modalWindow = document.querySelector('.ant-modal');
    expect(modalWindow).toBeTruthy();
    expect(isVisible).toBe(true);
    expect(deleteChain).toEqual(chain);


    document.querySelector('.ant-modal')?.querySelector('button.ant-btn-primary')?.dispatchEvent(new Event('click'));
    fixture.detectChanges();

    const action = new fromActions.DeleteChainAction(chain.id, chain.name);
    expect(store.dispatch).toHaveBeenCalledWith(action);
  });

  it('should change the sort order to descending', () => {
    component.sortDescription$.next({key: 'name', value: 'descend'});
    fixture.detectChanges();
    component.chainDataSorted$.subscribe(
      data => expect(data[0].name).toBe('Chain 3')
    );
  });

  it('should change the sort order to ascending', () => {
    component.sortDescription$.next({key: 'name', value: 'ascend'});
    fixture.detectChanges();
    component.chainDataSorted$.subscribe(
      data => expect(data[0].name).toBe('Chain 1')
    );
  });

  it('should call the show create modal on add button click', () => {
    spyOn(store, 'dispatch').and.callThrough();

    const addBtn = fixture.nativeElement.querySelector('[data-qe-id="add-chain-btn"]');
    addBtn.click();
    fixture.detectChanges();

    expect(store.dispatch).toHaveBeenCalledWith(new fromActions.ShowCreateModalAction());
  });

  it('should show create modal', () => {
    spyOn(store, 'dispatch').and.callThrough();

    store.overrideSelector(getCreateModalVisible, true);
    store.refreshState();
    fixture.detectChanges();

    const predicate = By.css('[data-qe-id="chain-name"]');
    const chainNameField: HTMLInputElement = fixture.debugElement.queryAll(predicate)[0].nativeElement;
    expect(chainNameField).toBeDefined();

    chainNameField.value = 'New Chain';
    chainNameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const createBtn = fixture.debugElement.queryAll(By.css('.ant-modal .ant-btn-primary'))[0].nativeElement;
    createBtn.click();
    fixture.detectChanges();
    const action = new fromActions.CreateChainAction({name: 'New Chain'});

    expect(store.dispatch).toHaveBeenCalledWith(action);
  });

  it('should call the pushValue function', () => {
    spyOn(store, 'dispatch').and.callThrough();

    store.overrideSelector(getCreateModalVisible, true);
    store.refreshState();
    fixture.detectChanges();

    const chainNameField: HTMLInputElement = fixture.debugElement.queryAll(By.css('[data-qe-id="chain-name"]'))[0].nativeElement;
    expect(chainNameField).toBeDefined();

    const pushMethodSpy = spyOn(component, 'pushChain');
    chainNameField.value = 'New Chain';
    chainNameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const createBtn = fixture.debugElement.queryAll(By.css('.ant-modal .ant-btn-primary'))[0].nativeElement;
    createBtn.click();
    fixture.detectChanges();
    expect(pushMethodSpy).toHaveBeenCalledTimes(1);
  });
});
