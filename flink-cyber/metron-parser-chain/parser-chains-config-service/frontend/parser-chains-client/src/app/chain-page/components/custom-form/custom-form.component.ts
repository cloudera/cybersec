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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';

export interface CustomFormConfig {
  name: string;
  type: string;
  id?: string;
  path?: string;
  multiple?: boolean;
  value?: string | any[];
  label?: string;
  options?: { id: string, name: string }[];
  onChange?: (config: any) => {};
  required?: boolean;
  multipleValues?: boolean;
  description?: string;
  placeholder?: string;
  defaultValue?: string;
}

@Component({
  selector: 'app-custom-form',
  templateUrl: './custom-form.component.html',
  styleUrls: ['./custom-form.component.scss']
})
export class CustomFormComponent implements OnInit, OnChanges {

  @Input() config: CustomFormConfig[] = [];
  @Output() valueChange = new EventEmitter<any>();

  formGroup: FormGroup;

  constructor() { }

  ngOnInit() {
    this.formGroup = new FormGroup(this.config.reduce((controls: any, fieldConfig) => {
      controls[fieldConfig.name] = new FormControl(fieldConfig.value);
      return controls;
    }, {}));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.config && changes.config.previousValue) {
      changes.config.previousValue.forEach((fieldConfig: any, i: number) => {
        const previousValue = changes.config.previousValue[i].value;
        const currentValue = changes.config.currentValue[i].value;
        const control : any = this.formGroup.get(fieldConfig.name);
        if (previousValue !== currentValue && control.value !== currentValue) {
          this.formGroup.removeControl(fieldConfig.name);
          this.formGroup.setControl(fieldConfig.name, new FormControl(currentValue));
        }
      });
    }
  }

  onChange(event: any, config: CustomFormConfig) {
    let value;
    switch (config.type) {
      case 'textarea':
      case 'text': {
        if (config.multiple !== true) {
          value = (event.currentTarget as HTMLFormElement).value;
        } else {
          value = event;
        }
        break;
      }
      case 'select': {
        value = event;
        break;
      }
    }
    this.valueChange.emit({
      ...config,
      value
    });
  }

  trackByFn(index: number) {
    return index;
  }
}
