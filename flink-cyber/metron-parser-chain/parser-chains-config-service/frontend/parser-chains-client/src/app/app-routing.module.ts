import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ChainAddParserPageComponent } from './chain-add-parser-page/chain-add-parser-page.component';
import { ChainListPageComponent } from './chain-list-page/chain-list-page.component';
import { ChainPageComponent } from './chain-page/chain-page.component';
import { CanDeactivateComponent } from './misc/can-deactivate-component';
import { MainContainerComponent } from './misc/main/main.container';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';

export const routes: Routes = [
  { path: '404', component: PageNotFoundComponent },
  { path: '', component: MainContainerComponent },
  { path: 'parserconfig', component: ChainListPageComponent },
  {
    path: 'parserconfig/chains/:id',
    component: ChainPageComponent,
    canDeactivate: [ CanDeactivateComponent ]
  },
  { path: 'parserconfig/chains/:id/new', component: ChainAddParserPageComponent },
  { path: ':type', component: MainContainerComponent },
  { path: '**', component: PageNotFoundComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

