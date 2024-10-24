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

import {
  AfterContentChecked,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges
} from '@angular/core';
import {UntypedFormControl} from '@angular/forms';
import {isObject} from 'src/app/shared/utils';
import {CustomFormConfig} from '../../custom-form.component';

@Component({
  selector: 'app-multi-input',
  templateUrl: './multi-input.component.html',
  styleUrls: ['./multi-input.component.scss']
})
export class MultiInputComponent implements OnInit, OnChanges, AfterContentChecked {

  @Input() config: CustomFormConfig;
  @Input() value: string | any[] = "";
  @Input() selectedSource: string;
  @Input() indexingFieldMap: {[key: string]: {[key:string]: boolean}};
  @Output() changeValue = new EventEmitter<{ [key: string]: string }[]>();
  controls = [];
  ignoreColumns: string[] = [];
  mappingColumns: string[] = [];
  selectSearchValue = "";

  constructor(private _changeDetector: ChangeDetectorRef) {
  }

  ngOnInit() {
    if (Array.isArray(this.value)) {
      this.controls = this.value.filter(item => !!item[this.config.name]).map(item =>
          new UntypedFormControl(item[this.config.name]));
    }
    if (this.controls.length === 0) {
      this.controls.push(
        new UntypedFormControl('')
      );
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.indexingFieldMap || changes.selectedSource) {
      this._updateDropdownLists();
    }
  }

  ngAfterContentChecked(): void {
    this._changeDetector.detectChanges();
  }

  onAddClick() {
    if (this.config.multipleValues ) {
      this.controls.push(
        new UntypedFormControl('')
      );
    }
  }

  onChange(config: CustomFormConfig) {
    const value = this.controls.map(control => {
      return {
        [config.name]: control.value
      };
    });
    this.changeValue.emit(value);
  }

  updateValue(selectedValue: string, control: UntypedFormControl, config: CustomFormConfig) {
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

  selectSearch($event: string) {
    this.selectSearchValue = $event;
  }

  private _updateDropdownLists() {
    // clear the existing lists
    this.ignoreColumns = [];
    this.mappingColumns = [];

    // split the items into two lists based on the boolean value
    if (this.indexingFieldMap && this.selectedSource) {
      const fields = this.indexingFieldMap[this.selectedSource];
      if (isObject(fields)) {
        Object.entries(fields).forEach(([key, value]) => {
          if (value) {
            this.ignoreColumns.push(key);
          } else {
            this.mappingColumns.push(key);
          }
        });
      }
    }
    this.ignoreColumns.sort()
    this.mappingColumns.sort()
  }
}
