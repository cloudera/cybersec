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
  onChange?: (config) => {};
  required?: boolean;
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
    this.formGroup = new FormGroup(this.config.reduce((controls, fieldConfig) => {
      controls[fieldConfig.name] = new FormControl(fieldConfig.value);
      return controls;
    }, {}));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.config && changes.config.previousValue) {
      changes.config.previousValue.forEach((fieldConfig, i) => {
        const previousValue = changes.config.previousValue[i].value;
        const currentValue = changes.config.currentValue[i].value;
        const control = this.formGroup.get(fieldConfig.name);
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

  trackByFn(index) {
    return index;
  }
}
