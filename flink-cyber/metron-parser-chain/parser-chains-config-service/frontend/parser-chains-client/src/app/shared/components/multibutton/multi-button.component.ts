import {Component, OnInit, Input, Output, EventEmitter, ViewChild} from '@angular/core';
import {
  HttpClient, HttpRequest,
  HttpEventType, HttpErrorResponse
} from '@angular/common/http';
import {Subscription, of} from 'rxjs';
import {catchError, last, map, tap, timeout} from 'rxjs/operators';
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


