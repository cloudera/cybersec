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

import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Observable} from 'rxjs';
import {ClusterModel, Job} from "./cluster-list-page.model";
import {ClusterService} from "../../services/cluster.service";
import {ThemePalette} from "@angular/material/core/common-behaviors/color";


@Component({
  selector: 'app-cluster-list',
  templateUrl: './cluster-list-page.component.html',
  styleUrls: ['./cluster-list-page.component.scss']
})
export class ClusterListPageComponent implements OnInit {

  clusters$: Observable<ClusterModel[]>;
  displayedColumns: string[] = ['id', 'name', 'status', 'version', 'branches'];
  checkBoxColor: ThemePalette = 'primary';
  constructor(
    private clusterService: ClusterService,
    private router: Router,
  ) {
    this.clusters$ = clusterService.getClusters();
  }


  ngOnInit() {
  }

  getBranches = (jobs: Job[]) => [...new Set(jobs.map(job => job.branch))].join(', ');

  goToDetailCluster = (clusterId: string | number) => {
    this.router.navigate(['cluster', clusterId]);

  }
}
