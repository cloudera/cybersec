import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HbaseRoutingModule } from './hbase-routing.module';
import { HbaseComponent } from './hbase.component';


@NgModule({
  declarations: [HbaseComponent],
  imports: [
    CommonModule,
    HbaseRoutingModule
  ]
})
export class HbaseModule { }
