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
import {By} from '@angular/platform-browser';
import {CheckCircleOutline, CloseCircleOutline, WarningFill} from '@ant-design/icons-angular/icons';
import {NzIconModule} from 'ng-zorro-antd/icon';
import {NzCardModule} from 'ng-zorro-antd/card'
import {NzResultModule} from 'ng-zorro-antd/result'
import {NzTimelineModule} from 'ng-zorro-antd/timeline'

import {ParserByParserComponent} from './parser-by-parser.component';
import {MockStore, provideMockStore} from "@ngrx/store/testing";
import {Store} from "@ngrx/store";
import {DiffPopupState} from "../diff-popup/diff-popup.reducers";
import {StackTraceComponent} from "../stack-trace/stack-trace.component";
import {DiffPopupComponent} from "../diff-popup/diff-popup.component";
import {NzModalModule} from "ng-zorro-antd/modal";
import {NzPopoverModule} from "ng-zorro-antd/popover";
import {FormsModule} from "@angular/forms";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {ShowDiffModalAction} from "../diff-popup/diff-popup.actions";
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";
import {MockComponent} from "ng-mocks";


describe('ParserByParserComponent', () => {
  let component: ParserByParserComponent;
  let fixture: ComponentFixture<ParserByParserComponent>;
  let store: MockStore<DiffPopupState>;
  let stackTraceComponent: StackTraceComponent;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        ParserByParserComponent,
        MockComponent(StackTraceComponent),
        MockComponent(DiffPopupComponent)
      ],
      imports: [
        FormsModule,
        NzModalModule,
        NzPopoverModule,
        NzCardModule,
        NzTimelineModule,
        NzResultModule,
        NoopAnimationsModule,
        NzIconModule.forRoot([CheckCircleOutline, CloseCircleOutline, WarningFill])
      ],
      providers: [
        provideMockStore({})
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParserByParserComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store) as MockStore<DiffPopupState>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit parser id to investigate when clicked on', () => {
    const investigatorSpy = spyOn(component.investigateParser, 'emit');

    component.parserResults = [
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
    const investigateParserBtn = fixture.debugElement.query(By.css('[data-qe-id="investigateParserBtn"]'));
    investigateParserBtn.nativeElement.click();
    expect(investigatorSpy).toHaveBeenCalledWith('1234');
  });

  it('should display empty message when parserResults is not returned', () => {
    component.parserResults = null;
    component.logMessage = 'this is a test error message';
    fixture.detectChanges();

    const emptyMessage = fixture.debugElement.query(By.css('.ant-result-title'));
    const logMessage = fixture.debugElement.query(By.css('[data-qe-id="logMessage"'));

    expect(emptyMessage.nativeElement.textContent).toContain(component.ERROR_DESCRIPTOR);

    component.parserResults = [];
    fixture.detectChanges();

    expect(emptyMessage.nativeElement.textContent).toContain(component.ERROR_DESCRIPTOR);
    expect(logMessage.nativeElement.textContent).toContain(component.logMessage);
  });

  it('should bind stack trace msg to stack trace component', () => {
    component.parserResults = [
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
    stackTraceComponent = fixture.debugElement.query(By.directive(StackTraceComponent)).componentInstance;
    fixture.detectChanges();

    expect(stackTraceComponent.stackTraceMsg).toContain('Fake Strack Trace Msg');
  });

  it('should shod diff', () => {
    spyOn(store, 'dispatch').and.callThrough();
    component.parserResults = [
      {
        output: {
          fooType: "foo",
          barType: "bar"
        },
        log: {
          type: 'info',
          message: 'this is a message',
          parserId: '1234',
          stackTrace: 'Fake Strack Trace Msg',
        }
      },
      {
        output: {
          fooType: "foo4",
          barType: "bar4"
        },
        log: {
          type: 'info',
          message: 'this is a message',
          parserId: '1234',
          stackTrace: 'Fake Str'
        }
      }
    ];
    component.diffOnly = true;
    fixture.detectChanges();

    const btns = fixture.debugElement.queryAll(By.css('.diff-param-button'));
    btns[0].nativeElement.click();
    fixture.detectChanges();

    expect(store.dispatch).toHaveBeenCalledWith(ShowDiffModalAction({previousDiffValue: 'bar', newDiffValue: 'bar4'}));

    btns[1].nativeElement.click();
    fixture.detectChanges();

    expect(store.dispatch).toHaveBeenCalledWith(ShowDiffModalAction({previousDiffValue: 'foo', newDiffValue: 'foo4'}));

  });
});
