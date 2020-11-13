import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NzModalService } from 'ng-zorro-antd/modal';

export interface ActionSpec {
  name: string;
  colour: string;
  tooltip: string;
  danger?: boolean;
}

@Component({
  selector: 'app-action-buttons',
  templateUrl: './action-buttons.component.html',
  styleUrls: ['./action-buttons.component.sass']
})
export class ActionButtonsComponent implements OnInit {

  @Input() actions: ActionSpec[] = [];
  @Input() actionsPermitted: string[] = [];

  @Output() action: EventEmitter<ActionSpec> = new EventEmitter();

  constructor(private modal: NzModalService) {}

  ngOnInit(): void {
  }

  onClick(act: ActionSpec): void {
    if (act.danger) {
      // run a confirmation on the action
      this.modal.confirm({
        nzTitle: '<i>Are you sure?</i>',
        nzContent: '<b>This action is potentially dangerous</b>',
        nzOnOk: () => this.action.emit(act),
        nzIconType: 'exclamation-circle'
      });
    } else {
      this.action.emit(act);
    }
  }

  disabled(act: ActionSpec): boolean {
    return this.actionsPermitted.findIndex(v => v === act.name) < 0;
  }
}
