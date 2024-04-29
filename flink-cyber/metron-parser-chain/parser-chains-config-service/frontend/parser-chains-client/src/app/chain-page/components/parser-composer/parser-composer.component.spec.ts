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
import { StoreModule } from '@ngrx/store';

import * as fromReducers from '../../chain-page.reducers';

import { ParserComposerComponent } from './parser-composer.component';
import {MockComponent} from "ng-mocks";
import {ParserComponent} from "../parser/parser.component";
import {RouterComponent} from "../router/router.component";



describe('ParserComposerComponent', () => {
  let component: ParserComposerComponent;
  let fixture: ComponentFixture<ParserComposerComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({
          'chain-page': fromReducers.reducer
        })
      ],
      declarations: [
        ParserComposerComponent,
        MockComponent(ParserComponent),
        MockComponent(RouterComponent)
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParserComposerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
