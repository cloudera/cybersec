import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { ParsersComponent } from "./parsers.component";
import { ChainAddParserPageComponent } from '../chain-add-parser-page/chain-add-parser-page.component';
import { ChainListPageComponent } from '../chain-list-page/chain-list-page.component';
import { CanDeactivateComponent } from '../misc/can-deactivate-component';

const routes: Routes = [
  { path: '', component: ChainListPageComponent },
  {
    path: 'parserconfig/chains/:id',
    component: ParsersComponent,
    canDeactivate: [ CanDeactivateComponent ]
  },
  { path: 'parserconfig/chains/:id/new', component: ChainAddParserPageComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ParsersRoutingModule { }
