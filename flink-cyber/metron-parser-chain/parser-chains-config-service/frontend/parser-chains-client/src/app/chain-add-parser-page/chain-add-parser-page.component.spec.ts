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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Store, StoreModule } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { of } from 'rxjs';

import * as fromChainPageReducers from '../chain-page/chain-page.reducers';

import { ChainAddParserPageComponent } from './chain-add-parser-page.component';
import { AddParserPageState, reducer } from './chain-add-parser-page.reducers';

const fakeActivatedRoute = {
  params: of({
    id: '456'
  })
};

describe('ChainAddParserPageComponent', () => {
  let component: ChainAddParserPageComponent;
  let fixture: ComponentFixture<ChainAddParserPageComponent>;
  let store: Store<AddParserPageState>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ChainAddParserPageComponent ],
      imports: [
        NzModalModule,
        FormsModule,
        ReactiveFormsModule,
        NoopAnimationsModule,
        StoreModule.forRoot({
          'chain-add-parser-page': reducer,
          'parsers': fromChainPageReducers.reducer
        }),
        RouterTestingModule
      ],
      providers: [
        provideMockStore({ initialState: {
          'chain-add-parser-page': {},
          'parsers': {
            chains: {
              456: {}
            }
          }
        } }),
        { provide: ActivatedRoute, useFactory: () => fakeActivatedRoute }
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ChainAddParserPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    store = TestBed.get(Store);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the main chain', () => {
    const spy = spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(spy).toHaveBeenCalled();
  });
});
