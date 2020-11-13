import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { RestListComponent } from './rest-list/rest-list.component';

const routes: Routes = [
  { path: '', component: RestListComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RestRoutingModule { }
