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
import {ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, Router} from '@angular/router';
import {EditFill, PlusOutline} from '@ant-design/icons-angular/icons';
import {Store} from '@ngrx/store';
import {NzModalModule} from 'ng-zorro-antd/modal';
import {NzIconModule} from 'ng-zorro-antd/icon';
import {of} from 'rxjs';

import * as fromActions from './chain-page.actions';
import {ChainPageComponent} from './chain-page.component';
import {NzCardModule} from "ng-zorro-antd/card";
import {provideMockStore} from "@ngrx/store/testing";
import {NzTableModule} from "ng-zorro-antd/table";
import {NzTabsModule} from 'ng-zorro-antd/tabs';
import {IndexingFormComponent} from "./components/indexing-form/indexing-form.component";
import {ChainViewComponent} from "./components/chain-view/chain-view.component";
import {MockComponent} from 'ng-mocks';
import {LiveViewComponent} from "./components/live-view/live-view.component";
import {NzBreadCrumbModule} from "ng-zorro-antd/breadcrumb";
import {NzPopoverModule} from "ng-zorro-antd/popover";
import {findEl} from "src/app/shared/test/test-helper";

describe('ChainPageComponent', () => {
  let component: ChainPageComponent;
  let fixture: ComponentFixture<ChainPageComponent>;
  let store: Store<ChainPageComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzTableModule,
        NzTabsModule,
        NzModalModule,
        NzCardModule,
        NzBreadCrumbModule,
        NoopAnimationsModule,
        ReactiveFormsModule,
        NzPopoverModule,
        NzIconModule.forRoot([EditFill, PlusOutline])
      ],
      declarations: [
        ChainPageComponent,
        MockComponent(ChainViewComponent),
        MockComponent(LiveViewComponent),
        MockComponent(IndexingFormComponent)
      ],
      providers: [
        provideMockStore({
          initialState: {
            'chain-page': {
              chains:
                {
                  123: {
                    id: '123',
                    name: 'chain',
                    parsers: []
                  }
                },
              dirtyParsers: [],
              dirtyChains: [],
              path: [],
              parserToBeInvestigated: ""
            },
            'live-view': {
              chainConfig: {}
            },
          }
        }),
        {provide: ActivatedRoute, useValue: {params: of({id: '123'})}},
        {provide: Router, useValue: {events: of({})}},
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.inject(Store);
    fixture = TestBed.createComponent(ChainPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the popconfirm textbox for updating the chain name', () => {
    fixture.detectChanges();
    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    expect(editBtn).toBeTruthy();
    fixture.detectChanges();

    const nameField = document.querySelector('[data-qe-id="chain-name-field"]');
    expect(nameField).toBeTruthy();
  });

  it('should disable the chain name set btn if input length < 3', () => {
    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'aa';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    expect(submitBtn.disabled).toBe(true);
  });

  it('should enable the chain name set btn if input length > 3', () => {
    findEl(fixture, 'chain-name-edit-btn').nativeElement.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'aaa';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    expect(submitBtn.disabled).toBe(false);
  });

  it('should call the onChainNameEditDone()', () => {
    spyOn(component, 'onChainNameEditDone');

    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'hello';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    submitBtn.click();
    fixture.detectChanges();

    expect(component.onChainNameEditDone).toHaveBeenCalled();
  });

  it('onChainNameEditDone() will call the UpdateChain and SetDirty Actions', () => {
    spyOn(store, 'dispatch');

    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'new_name';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    submitBtn.click();
    fixture.detectChanges();

    const actionUpdate = new fromActions.UpdateChainAction({chain: {id: '123', name: 'new_name'}});

    expect(store.dispatch).toHaveBeenCalledWith(actionUpdate);
  });

  it('should pass the id of a failed parser if investigated', () => {
    component.parserToBeInvestigated = ['1111'];
    fixture.detectChanges();
    expect(component.parsers).toBe(component.parserToBeInvestigated);

    component.parserToBeInvestigated = [];
    component.chain.parsers = [{
      id: '123',
      type: 'test type',
      name: 'test name'
    }];
    fixture.detectChanges();
    expect(component.parsers).toBe(component.chain.parsers);
  });

});
