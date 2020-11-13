import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AsnComponent } from './asn/asn.component';
import { GeoComponent } from './geo/geo.component';

const routes: Routes = [
  { path: 'geo', component: GeoComponent },
  { path: 'asn', component: AsnComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MaxmindRoutingModule { }
