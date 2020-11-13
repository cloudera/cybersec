import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ParsersRoutingModule } from './parsers-routing.module';
import { ParsersComponent } from './parsers.component';


@NgModule({
  declarations: [ParsersComponent],
  imports: [
    CommonModule,
    ParsersRoutingModule
  ]
})
export class ParsersModule { }
