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
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';

import { SampleDataType } from '../models/sample-data.model';

import { SampleDataFormComponent } from './sample-data-form.component';

export class MockService {}

describe('SampleDataFormComponent', () => {
  let component: SampleDataFormComponent;
  let fixture: ComponentFixture<SampleDataFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        NzFormModule,
        NzButtonModule,
        NzInputModule,
      ],
      declarations: [ SampleDataFormComponent ],
      providers: [{ provide: NzMessageService, useClass: MockService}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataFormComponent);
    component = fixture.componentInstance;

    component.sampleData = {
      type: SampleDataType.MANUAL,
      source: '',
    };

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch change action', () => {
    const sampleDataInput = fixture.debugElement.query(By.css('[data-qe-id="sample-input"]')).nativeElement;
    const expected = {
      type: SampleDataType.MANUAL,
      source: 'test sample data',
    };

    component.sampleDataChange.subscribe(sampleData => {
      expect(sampleData).toEqual(expected);
    });

    sampleDataInput.value = 'test sample data';
    sampleDataInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  });
});
