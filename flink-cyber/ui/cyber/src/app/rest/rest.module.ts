import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { RestRoutingModule } from './rest-routing.module';
import { RestListComponent } from './rest-list/rest-list.component';
import { NzTableModule } from 'ng-zorro-antd/table';


@NgModule({
  declarations: [RestListComponent],
  imports: [
    CommonModule,
    RestRoutingModule,
    NzTableModule
  ]
})
export class RestModule { }
