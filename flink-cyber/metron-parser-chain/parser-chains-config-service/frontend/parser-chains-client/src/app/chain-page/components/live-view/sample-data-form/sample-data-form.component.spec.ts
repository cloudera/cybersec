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

import { ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';

import {SampleDataModel, SampleDataType} from '../models/sample-data.model';

import { SampleDataFormComponent } from './sample-data-form.component';
import {NzTabsModule} from "ng-zorro-antd/tabs";
import {MockComponent} from "ng-mocks";
import {SampleDataTextInputComponent} from "./sample-data-text-input/sample-data-text-input.component";
import {
  SampleDataTextFolderInputComponent
} from "./sample-data-text-folder-input/sample-data-text-folder-input.component";


describe('SampleDataFormComponent', () => {
  let component: SampleDataFormComponent;
  let fixture: ComponentFixture<SampleDataFormComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzTabsModule,
        FormsModule,
        NzFormModule,
        NzButtonModule,
        NzInputModule,
      ],
      declarations: [
        SampleDataFormComponent,
        MockComponent(SampleDataTextInputComponent),
        MockComponent(SampleDataTextFolderInputComponent)
      ],
      providers: [ NzMessageService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch change action', (done) => {
    component.sampleDataChange.subscribe(sampleData => {
      expect(sampleData).toEqual(expected);
      done();
    });
    const expected: SampleDataModel = {
      type: SampleDataType.MANUAL,
      source: 'test sample data',
    };

    const sampleDataTextInput: SampleDataTextInputComponent = fixture.debugElement.query(By.directive(SampleDataTextInputComponent)).componentInstance;
    sampleDataTextInput.sampleDataChange.emit(expected);
  });

  it('should dispatch change action from SampleDataFolder', (done) => {
    fixture.debugElement.queryAll(By.css('.ant-tabs-tab-btn'))[1].nativeElement.click();
    fixture.detectChanges();
    component.sampleDataChange.subscribe(sampleData => {
      expect(sampleData).toEqual(expected);
      done();
    });
    const expected: SampleDataModel = {
      type: SampleDataType.MANUAL,
      source: 'test sample data',
    };
    const sampleDataFolderInput: SampleDataTextFolderInputComponent = fixture.debugElement.query(By.directive(SampleDataTextFolderInputComponent)).componentInstance;
    sampleDataFolderInput.sampleDataChange.emit(expected);
  });
});
