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

import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {FormControl} from '@angular/forms';

import {CustomFormConfig} from '../../custom-form.component';

@Component({
  selector: 'app-multi-input',
  templateUrl: './multi-input.component.html',
  styleUrls: ['./multi-input.component.scss']
})
export class MultiInputComponent implements OnInit, OnChanges {

  @Input() config: CustomFormConfig;
  @Input() value: string | any[] = "";
  @Input() indexingFieldMap: Map<string,boolean>;
  @Output() changeValue = new EventEmitter<{ [key: string]: string }[]>();

  count = 0;
  controls = [];
  ignoreColumns: string[] = [];
  mappingColumns: string[] = [];

  constructor() { }

  ngOnInit() {
    if (Array.isArray(this.value)) {
      this.controls = this.value.filter(item => !!item[this.config.name]).map(item =>
          new FormControl(item[this.config.name]));
    }
    if (this.controls.length === 0) {
      this.controls.push(
        new FormControl('')
      );
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    console.log('changes')
    if (changes.indexingFieldMap) {
      console.log('map changes')
      this.updateDropdownLists();
    }
  }

  private updateDropdownLists() {
    // clear the existing lists
    this.ignoreColumns = [];
    this.mappingColumns = [];

    // split the items into two lists based on the boolean value
    this.indexingFieldMap.forEach((value, key) => {
      if (value) {
        this.ignoreColumns.push(key);
      } else {
        this.mappingColumns.push(key);
      }
    });
    this.ignoreColumns.sort()
    this.mappingColumns.sort()
  }

  onAddClick() {
    this.controls.push(
      new FormControl('')
    );
  }

  onChange(config: CustomFormConfig) {
    const value = this.controls.map(control => {
      return {
        [config.name]: control.value
      };
    });
    this.changeValue.emit(value);
  }

  updateValue(selectedValue: String, control: FormControl, config: CustomFormConfig) {
    control.setValue(selectedValue)
    this.onChange(config)
  }

  onRemoveFieldClick(control, config) {
    this.controls = this.controls.filter((item) => {
      return item !== control;
    });
    const value = this.controls.map(item => {
      return {
        [config.name]: item.value
      };
    });
    this.changeValue.emit(value);
  }
}
