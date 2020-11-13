import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { RulesListComponent } from './rules-list/rules-list.component';
import { RulesEditorComponent } from './rules-editor/rules-editor.component';

const routes: Routes = [
  { path: '', component: RulesListComponent },
  { path: '{id}', component: RulesEditorComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RulesRoutingModule { }
