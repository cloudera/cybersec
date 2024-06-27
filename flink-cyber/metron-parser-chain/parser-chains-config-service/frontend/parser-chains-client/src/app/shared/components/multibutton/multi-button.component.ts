import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {MatButtonToggleGroup} from "@angular/material/button-toggle";

export type MultiButton = {
  label: string,
  value: string,
  disabled: boolean
}

@Component({
  selector: 'app-multi-button',
  templateUrl: './multi-button.component.html',
  styleUrls: ['./multi-button.component.scss'],
  animations: [
  ]
})
export class MultiButtonComponent  {
  @Input() buttons: MultiButton[]      = [
    {label: 'Archive', value: 'Archive', disabled: false},
    {label: 'Git', value: 'Git', disabled: false},
    {label: 'Manual', value: 'Manual', disabled: false},
  ];
  @Input() defaultValue: string;

  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('group') group: MatButtonToggleGroup;

  onValueChange() {
    this.valueChange.emit(this.group.value);
  }
}


