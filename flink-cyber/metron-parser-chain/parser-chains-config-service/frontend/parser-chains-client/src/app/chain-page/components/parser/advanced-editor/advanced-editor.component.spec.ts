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

import { AdvancedEditorComponent } from './advanced-editor.component';

describe('AdvancedEditorComponent', () => {
  let component: AdvancedEditorComponent;
  let fixture: ComponentFixture<AdvancedEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
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
    const mockListener = jasmine.createSpy('mockListener');
    component.configChanged.subscribe(mockListener);

    component.onChange('{ "someField": "some value" }');

    expect(mockListener).toHaveBeenCalledWith({ value: JSON.parse('{ "someField": "some value" }') });
  });

  it('should ignore content if not valid json', () => {
    const mockListener = jasmine.createSpy('mockListener');
    component.configChanged.subscribe(mockListener);

    component.onChange('not a json, but sure it can be typed in to the editor');

    expect(mockListener).not.toHaveBeenCalledWith();
  });

  it('should not emit event if new value equals with the original', () => {
    const mockListener = jasmine.createSpy('mockListener');
    component.config = { initialField: 'initial value' };
    component.configChanged.subscribe(mockListener);

    component.onChange('{ initialField: "initial value" }');

    expect(mockListener).not.toHaveBeenCalledWith();
  });

});
