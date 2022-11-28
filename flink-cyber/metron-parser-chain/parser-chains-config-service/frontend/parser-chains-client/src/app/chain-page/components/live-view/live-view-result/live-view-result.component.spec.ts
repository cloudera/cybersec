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
import { By } from '@angular/platform-browser';
import { NzCardModule } from 'ng-zorro-antd/card';

import { LiveViewResultComponent } from './live-view-result.component';

@Component({
  selector: 'app-parser-by-parser',
  template: '',
})
export class MockParserByParserComponent {
  @Input() parserResults = [];
  @Input() logMessage = 'test';
}

@Component({
  selector: 'app-stack-trace',
  template: '',
})
class FakeStackTraceComponent {
  @Input() stackTraceMsg = '';
}

describe('LiveViewResultComponent', () => {
  let component: LiveViewResultComponent;
  let fixture: ComponentFixture<LiveViewResultComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        LiveViewResultComponent,
        MockParserByParserComponent,
        FakeStackTraceComponent,
      ],
      imports: [ NzCardModule ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LiveViewResultComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should bind stack trace msg to stack trace component', () => {
    component.results =  [
      {
        output: {},
        log: {
          type: 'info',
          message: 'this is a message',
          parserId: '1234',
          stackTrace: 'Fake Strack Trace Msg',
        }
      }
    ];
    fixture.detectChanges();

    const stackTraceComp = fixture.debugElement.query(
      By.directive(FakeStackTraceComponent)
      ).componentInstance;

    expect(stackTraceComp.stackTraceMsg).toBe('Fake Strack Trace Msg');
  });
});
