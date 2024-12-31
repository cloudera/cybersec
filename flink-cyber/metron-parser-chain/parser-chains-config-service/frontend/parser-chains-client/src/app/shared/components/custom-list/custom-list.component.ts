import {Component, HostBinding, Input, ViewEncapsulation} from '@angular/core';
import {coerceBooleanProperty} from '@angular/cdk/coercion';

@Component({
  selector: 'app-custom-list',
  exportAs: 'assignFieldList',
  template: '<ng-content></ng-content>',
  styleUrls: ['./custom-list.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class CustomListComponent{
  private _disabled = false;

  @HostBinding('class') class = 'custom-list custom-list-base';

  get disabled()  {
    return this._disabled;
  }

  @Input()
  set disabled(value: any) {
    this._disabled = coerceBooleanProperty(value);
  }
}
