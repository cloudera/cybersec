import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { EnrichmentsRoutingModule } from './enrichments-routing.module';
import { EnrichmentsComponent } from './enrichments.component';


@NgModule({
  declarations: [EnrichmentsComponent],
  imports: [
    CommonModule,
    EnrichmentsRoutingModule
  ]
})
export class EnrichmentsModule { }
