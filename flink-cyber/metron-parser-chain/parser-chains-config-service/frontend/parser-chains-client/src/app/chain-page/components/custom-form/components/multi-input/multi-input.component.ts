import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';

import { CustomFormConfig } from '../../custom-form.component';

@Component({
  selector: 'app-multi-input',
  templateUrl: './multi-input.component.html',
  styleUrls: ['./multi-input.component.scss']
})
export class MultiInputComponent implements OnInit {

  @Input() config: CustomFormConfig;
  @Input() value: { [key: string]: string }[] = [];
  @Output() changeValue = new EventEmitter<{ [key: string]: string }[]>();

  count = 0;
  controls = [];

  constructor() { }

  ngOnInit() {
    this.controls = (this.value || []).map(item => {
      if (item[this.config.name]) {
        return new FormControl(item[this.config.name]);
      }
      return null;
    }).filter(Boolean);

    if (this.controls.length === 0) {
      this.controls.push(
        new FormControl('')
      );
    }
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
