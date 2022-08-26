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
  @Input() value: string | any[] = "";
  @Output() changeValue = new EventEmitter<{ [key: string]: string }[]>();

  count = 0;
  controls = [];

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
