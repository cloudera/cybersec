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

import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {BehaviorSubject, Observable} from 'rxjs';
import {ClusterModel, Job} from "./cluster-list-page.model";
import {ClusterService} from "../../services/cluster.service";
import {tap} from "rxjs/operators";


@Component({
  selector: 'app-cluster-list',
  templateUrl: './cluster-list-page.component.html',
  styleUrls: ['./cluster-list-page.component.scss']
})
export class ClusterListPageComponent {
  clusters$: Observable<ClusterModel[]> =    this._clusterService.getClusters().pipe(tap(() => this.isLoading$.next(false)));
  displayedColumns: string[] = ['id', 'name', 'status', 'version', 'branches'];
  isLoading$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(true);

  constructor(
    private _clusterService: ClusterService,
    private _router: Router,
  ) {
  }

  getBranches = (jobs: Job[]) => [...new Set(jobs.map(job => job.jobPipeline))].join(', ');

  goToDetailCluster = (clusterId: string | number) => {
    this._router.navigate(['clusters', clusterId]);
  }
}
