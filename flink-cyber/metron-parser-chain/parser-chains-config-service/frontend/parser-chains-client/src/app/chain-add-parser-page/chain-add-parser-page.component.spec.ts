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

import {ComponentFixture, fakeAsync, flush, TestBed, tick, waitForAsync} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {Store} from '@ngrx/store';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {NzModalModule} from 'ng-zorro-antd/modal';
import {of} from 'rxjs';

import {ChainAddParserPageComponent} from './chain-add-parser-page.component';
import {AddParserPageState, getParserTypes, initialState} from './chain-add-parser-page.reducers';
import {NzCardModule} from "ng-zorro-antd/card";
import {NzFormModule} from "ng-zorro-antd/form";
import {CommonModule} from "@angular/common";
import {NzSelectModule} from "ng-zorro-antd/select";
import {getChain} from "../chain-page/chain-page.reducers";
import * as fromActions from "./chain-add-parser-page.actions";
import {setFieldValue} from "../shared/test/test-helper";
import {NzIconTestModule} from "ng-zorro-antd/icon/testing";
import {By} from "@angular/platform-browser";

function addParserCall(component: ChainAddParserPageComponent, expectChainId: string, expectSubchainId: string, fixture: ComponentFixture<ChainAddParserPageComponent>) {
  component.chainId = expectChainId;
  component.subchainId = expectSubchainId;
  const expectedParser = {
    ...component.addParserForm.value,
    id: jasmine.any(String),
    config: {},
  };
  expectedParser.routing = {};

  component.addParser();
  fixture.detectChanges();
  return expectedParser;
}

const typesList = [
  {id: 'Router', name: 'foo-type'},
  {id: 'Router2', name: 'foo-type2'},
  {id: 'Router3', name: 'foo-type3'}
];
const testChain = {id: '123', name: 'fooTestChain', parsers: [{id: '1', name: 'barParser'}]};


describe('ChainAddParserPageComponent', () => {
  let component: ChainAddParserPageComponent;
  let fixture: ComponentFixture<ChainAddParserPageComponent>;
  let store: MockStore<AddParserPageState>;
  let routerSpy: Router;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ChainAddParserPageComponent],
      imports: [
        NzModalModule,
        NzSelectModule,
        FormsModule,
        CommonModule,
        ReactiveFormsModule,
        NoopAnimationsModule,
        RouterTestingModule,
        NzCardModule,
        NzFormModule,
        NzIconTestModule
      ],
      providers: [
        provideMockStore(
          {
            initialState: {
              parserTypes: typesList,
              error: '',
              'chain-page': {
                chains: {
                  [testChain.id]: testChain
                }
              }
            },
            selectors: [
              {selector: getChain({id: testChain.id}), value: testChain},
              {
                selector: getParserTypes, value: typesList
              }
            ]
          }),
        {provide: ActivatedRoute, useValue: {params: of({id: testChain.id, chainId: testChain.name})}},
        {provide: Router, useValue: jasmine.createSpyObj('Router', ['navigateByUrl'])}
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ChainAddParserPageComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store) as MockStore<AddParserPageState>;
    routerSpy = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the main chain', () => {
    const spy = spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(spy).toHaveBeenCalled();
  });

  it('should dispatch AddParserAction with subchain if exist', fakeAsync(() => {
    const dispatchSpy = spyOn(store, 'dispatch').and.callThrough();
    routerSpy.navigateByUrl = jasmine.createSpy('navigateByUrl').and.callThrough();
    const expectChainId = 'fooTestChain';
    const expectSubchainId = "testSubchainId";

    setFieldValue(fixture, 'name', 'SomeName');
    const nzSelect = fixture.debugElement.query(By.css('nz-select'));
    nzSelect.nativeElement.querySelector("input").dispatchEvent(new KeyboardEvent('keydown', {
      key: ' ',
      bubbles: true
    }));
    tick(20);
    fixture.detectChanges();
    expect(component.addParserForm.value.type).toBeNull();
    expect(component.addParserForm.value.name).toBe('SomeName');

    const nzOptions = nzSelect.queryAll(By.css('nz-option-item'));
    expect(nzOptions.length).toBe(typesList.length);
    nzOptions[0].nativeElement.click();
    fixture.detectChanges();
    expect(component.addParserForm.value.type).toBe(typesList[0].id);

    const expectedParser = addParserCall(component, expectChainId, expectSubchainId, fixture);

    expect(dispatchSpy).toHaveBeenCalledWith(
      new fromActions.AddParserAction({
        chainId: expectSubchainId,
        parser: expectedParser
      })
    );
    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith(`/parserconfig/chains/${expectChainId}`);
    flush();
  }));
});
