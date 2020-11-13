import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MaxmindRoutingModule } from './maxmind-routing.module';
import { GeoComponent } from './geo/geo.component';
import { AsnComponent } from './asn/asn.component';


@NgModule({
  declarations: [GeoComponent, AsnComponent],
  imports: [
    CommonModule,
    MaxmindRoutingModule
  ]
})
export class MaxmindModule { }
