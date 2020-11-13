import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActionButtonsComponent } from './action-buttons/action-buttons.component';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzButtonModule } from 'ng-zorro-antd/button';

@NgModule({
  declarations: [ActionButtonsComponent],
  imports: [
    CommonModule,
    NzModalModule,
    NzButtonModule
  ],
  exports: [ActionButtonsComponent ]
})
export class SharedModule { }
