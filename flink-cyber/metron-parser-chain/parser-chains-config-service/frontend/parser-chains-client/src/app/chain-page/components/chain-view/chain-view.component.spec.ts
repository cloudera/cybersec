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

import {HttpClientTestingModule} from '@angular/common/http/testing';
import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {FormsModule, NgControl, ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {NzMessageService, NzMessageServiceModule} from 'ng-zorro-antd/message';

import {ParserModel} from '../../chain-page.models';

import {ChainViewComponent} from './chain-view.component';
import {NzSelectModule} from "ng-zorro-antd/select";
import {ChainPageService} from "../../../services/chain-page.service";
import {of} from "rxjs";
import {ParserComposerComponent} from "../parser-composer/parser-composer.component";
import {MockComponent} from "ng-mocks";

describe('ChainViewComponent', () => {
  let component: ChainViewComponent;
  let fixture: ComponentFixture<ChainViewComponent>;
  const parsers: ParserModel[] = [
    {
      id: '123',
      name: 'Syslog',
      type: 'Grok',
      config: {},
    }
  ];

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        ChainViewComponent,
        MockComponent(ParserComposerComponent)
      ],
      imports: [
        NzMessageServiceModule,
        NoopAnimationsModule,
        ReactiveFormsModule,
        HttpClientTestingModule,
        FormsModule,
        NzSelectModule
      ],
      providers: [
        {
          provide: ChainPageService,
          useValue: {
            collapseAll: of(true),
            createChainCollapseArray: jasmine.createSpy(),
            getCollapseExpandState: () => of([true, false, true]),
            collapseExpandAllParsers: jasmine.createSpy(),
          },
        },
        NzMessageService,
        NgControl
      ],

    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ChainViewComponent);
    component = fixture.componentInstance;
    component.parsers = parsers;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
