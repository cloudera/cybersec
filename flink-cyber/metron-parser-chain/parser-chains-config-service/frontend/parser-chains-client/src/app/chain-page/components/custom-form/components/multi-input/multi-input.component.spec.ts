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

import {async, ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {FormControl, FormsModule, UntypedFormControl, ReactiveFormsModule} from '@angular/forms';
import { MinusCircleFill, PlusCircleFill } from '@ant-design/icons-angular/icons';
import { NzModalModule } from 'ng-zorro-antd/modal';
import {NzIconModule} from 'ng-zorro-antd/icon';
import { MultiInputComponent } from './multi-input.component';
import {NzToolTipModule} from "ng-zorro-antd/tooltip";
import {NzMessageService} from "ng-zorro-antd/message";
import {NzPopconfirmModule} from "ng-zorro-antd/popconfirm";
import {NzFormModule} from "ng-zorro-antd/form";
import {NzInputModule} from "ng-zorro-antd/input";
import {NzButtonModule} from "ng-zorro-antd/button";
import {CommonModule} from "@angular/common";
import {NzLayoutModule} from "ng-zorro-antd/layout";

describe('MultiInputComponent', () => {
  let component: MultiInputComponent;
  let fixture: ComponentFixture<MultiInputComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        NzFormModule,
        NzInputModule,
        NzButtonModule,
        CommonModule,
        FormsModule,
        NzToolTipModule,
        NzIconModule.forChild([PlusCircleFill, MinusCircleFill]),
        NzPopconfirmModule,
        ReactiveFormsModule,
        NzLayoutModule,
      ],
      declarations: [ MultiInputComponent ],
      providers: [
        NzMessageService
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MultiInputComponent);
    component = fixture.componentInstance;
    component.config = { name: 'foo', type: 'text' };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should add one form control', () => {
    component.config = {
      type: 'text',
      name: 'foo',
      multipleValues: true
    };
    expect(component.controls.length).toBe(1);
    expect(component.controls[0].value).toEqual('');
    component.onAddClick();
    fixture.detectChanges();
    expect(component.controls.length).toBe(2);
    expect(component.controls[0].value).toEqual('');
    expect(component.controls[1].value).toEqual('');
    component.onAddClick();
    fixture.detectChanges();
    expect(component.controls.length).toBe(3);
    expect(component.controls[0].value).toEqual('');
    expect(component.controls[1].value).toEqual('');
    expect(component.controls[2].value).toEqual('');
  });

  it('should emit change with the proper payload', () => {
    const spy = spyOn(component.changeValue, 'emit');
    component.controls = [
      new UntypedFormControl('value 1'),
      new UntypedFormControl('value 2'),
      new UntypedFormControl('value 3'),
    ];
    component.onChange({
      type: 'text',
      name: 'foo'
    });
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith([{
      foo: 'value 1'
    }, {
      foo: 'value 2'
    }, {
      foo: 'value 3'
    }]);
  });

  it('should remove one form control', () => {
    const spy = spyOn(component.changeValue, 'emit');
    component.config = {
      type: 'text',
      name: 'foo',
      multipleValues: true
    };
    expect(component.controls.length).toBe(1);

    component.onAddClick();
    component.onAddClick();
    fixture.detectChanges();

    expect(component.controls.length).toBe(3);

    const controlToBeRemoved = component.controls[0];
    component.onRemoveFieldClick(controlToBeRemoved, component.config);
    fixture.detectChanges();

    expect(component.controls.length).toBe(2);

    expect(component.controls.find((c) => c === controlToBeRemoved))
      .toBeUndefined();

    expect(spy).toHaveBeenCalledWith([{
      foo: ''
    }, {
      foo: ''
    }]);
  });
});
