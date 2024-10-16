/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {ChainAddParserPageComponent} from './chain-add-parser-page/chain-add-parser-page.component';
import {ChainListPageComponent} from './chain-list-page/chain-list-page.component';
import {ChainPageComponent} from './chain-page/chain-page.component';
import {CanDeactivateComponent} from './misc/can-deactivate-component';
import {MainContainerComponent} from './misc/main/main.container';
import {PageNotFoundComponent} from './page-not-found/page-not-found.component';
import {ClusterListPageComponent} from "./cluster/cluster-list-page/cluster-list-page.component";
import {ClusterPageComponent} from "./cluster/cluster-page/cluster-page.component";
import {PipelinesComponent} from 'src/app/cluster/pipelines/pipelines.component';
import {PipelineCreateComponent} from 'src/app/cluster/pipelines/pipeline-create/pipeline-create.component';
import {PipelineSubmitComponent} from 'src/app/cluster/pipelines/pipeline-submit/pipeline-submit.component';

export const routes: Routes = [
  { path: '404', component: PageNotFoundComponent },
  { path: '', component: MainContainerComponent },
  { path: 'parserconfig', component: ChainListPageComponent },
  {
    path: 'parserconfig/chains/:id',
    component: ChainPageComponent,
    canDeactivate: [ CanDeactivateComponent ]
  },
  { path: 'clusters', component: ClusterListPageComponent },
  { path: 'clusters/pipelines', component: PipelinesComponent },
  { path: 'clusters/pipelines/create', component: PipelineCreateComponent },
  { path: 'clusters/pipelines/submit', component: PipelineSubmitComponent },
  { path: 'clusters/:clusterId', component: ClusterPageComponent},

  { path: 'parserconfig/chains/:id/new', component: ChainAddParserPageComponent },
  { path: ':type', component: MainContainerComponent },
  { path: '**', component: PageNotFoundComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

