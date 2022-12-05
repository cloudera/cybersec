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

import { Component, Input } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreModule } from '@ngrx/store';

import * as fromReducers from '../../chain-page.reducers';

import { ParserComposerComponent } from './parser-composer.component';

@Component({
  selector: 'app-parser',
  template: ''
})
class MockParserComponent {
  @Input() dirty = false;
  @Input() parser : any;
  @Input() configForm : any;
  @Input() isolatedParserView : any;
  @Input() parserType : any;
  @Input() failedParser : any;
  @Input() collapsed : any;
}

@Component({
  selector: 'app-router',
  template: ''
})
class MockRouterComponent extends MockParserComponent {}

describe('ParserComposerComponent', () => {
  let component: ParserComposerComponent;
  let fixture: ComponentFixture<ParserComposerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({
          'chain-page': fromReducers.reducer
        })
      ],
      declarations: [
        ParserComposerComponent,
        MockParserComponent,
        MockRouterComponent
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
