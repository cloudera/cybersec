import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {MatButtonToggleGroup} from "@angular/material/button-toggle";

type button = {
  label: string,
  value: string,
}

@Component({
  selector: 'app-multi-button',
  templateUrl: './multi-button.component.html',
  styleUrls: ['./multi-button.component.scss'],
  animations: [
  ]
})
export class MultiButtonComponent  {
  @Input() buttons: button[]      = [
    {label: 'Archive', value: 'Archive'},
    {label: 'Git', value: 'Git'},
    {label: 'Manual', value: 'Manual'},
  ];

  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('group') group: MatButtonToggleGroup;

  onValueChange() {
    this.valueChange.emit(this.group.value);
  }

}


