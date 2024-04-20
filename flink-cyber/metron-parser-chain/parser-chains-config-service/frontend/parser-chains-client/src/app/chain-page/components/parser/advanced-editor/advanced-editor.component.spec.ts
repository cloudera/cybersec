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
import { FormsModule } from '@angular/forms';

import { AdvancedEditorComponent } from './advanced-editor.component';
import {NzFormModule} from "ng-zorro-antd/form";
import {MonacoEditorModule} from "ngx-monaco-editor-v2";

describe('AdvancedEditorComponent', () => {
  let component: AdvancedEditorComponent;
  let fixture: ComponentFixture<AdvancedEditorComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MonacoEditorModule.forRoot({}),
        NzFormModule,
      ],
      declarations: [ AdvancedEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch event when value changes', () => {
    spyOn(component.configChanged, 'emit');


    component.onChange('{"someField": {"foo": "bar"}}');

    expect(component.configChanged.emit).toHaveBeenCalledWith({ value: JSON.parse('{"someField": {"foo": "bar"}}') });
  });

  it('should ignore content if not valid json', () => {
    spyOn(component.configChanged, 'emit');

    component.onChange('not a json, but sure it can be typed in to the editor');

    expect(component.configChanged.emit).not.toHaveBeenCalledWith();
  });

  it('should not emit event if new value equals with the original', () => {
    spyOn(component.configChanged, 'emit');
    component.config = { initialField: {foo: 'bar'} };

    component.onChange('{"initialField": {"foo": "bar"}}');

    expect(component.configChanged.emit).not.toHaveBeenCalledWith();
  });

});
