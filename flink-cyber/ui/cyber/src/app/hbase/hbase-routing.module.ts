import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { HbaseComponent } from './hbase.component';

const routes: Routes = [
  { path: '', component: HbaseComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HbaseRoutingModule { }
