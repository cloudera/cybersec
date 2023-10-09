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
import {ActivatedRoute, Router} from '@angular/router';
import {forkJoin, Observable, of, throwError} from 'rxjs';
import {catchError, switchMap} from "rxjs/operators";
import {ClusterModel, Job} from "../cluster-list-page/cluster-list-page.model";
import {ClusterService} from "../../services/cluster.service";
import {SelectionModel} from '@angular/cdk/collections';
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {SnackbarService, SnackBarStatus} from "../../services/snack-bar.service";


@Component({
  selector: 'app-cluster',
  templateUrl: './cluster-page.component.html',
  styleUrls: ['./cluster-page.component.scss']
})
export class ClusterPageComponent implements OnInit {
  jobs: Job[];
  cluster$!: Observable<ClusterModel>;
  clusterId = '0';
  displayedColumns: string[] = ['select', 'name', 'branch', 'pipeline', 'status', 'created'];
  selection = new SelectionModel<Job>(true, []);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private clusterService: ClusterService,
    private snackBarService: SnackbarService
  ) {
    this.cluster$ = this.route.paramMap.pipe(
      switchMap(params => {
        this.clusterId = params.get('clusterId');
        return this.clusterService.getCluster(this.clusterId);
      })
    );
    this.cluster$.subscribe(cluster => {
      this.jobs = cluster.jobs;
    })
  }

  ngOnInit() {
  }

  isAllSelected() {
    return this.selection.selected.length === this.jobs?.length;
  }

  masterToggle() {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.selection.select(...this.jobs);
    }
  }

  changeJobStatus(newStatus: 'running' | 'started' | 'stopped') {
    forkJoin(
      this.selection.selected.reduce((acc, job) =>
          ({
            ...acc,
            [job.name]:
              this.clusterService.sendJobCommand(this.clusterId, job.name, {status: newStatus}).pipe(catchError(err => {
                this.snackBarService.showMessage(err.message, SnackBarStatus.Fail);
                return of(null);
              }))
          }),
        {})
    ).subscribe((value: { [jobName: string]: HttpResponse<any> }) => {
        Object.entries(value).forEach(([jobName, res]) => {
          this.jobs = this.jobs.map(this.updateJobStatus(jobName, res, newStatus));
        });
      },
      (error) => {
        //handle your error here
        this.snackBarService.showMessage(`Unexpected error ${error.message}`, SnackBarStatus.Fail);
      }, () => {
        //observable completes
        this.selection.clear();
      });
  }

  private updateJobStatus(jobName: string, res: HttpResponse<any>, newStatus: 'running' | 'started' | 'stopped') {
    return job => {
      if (job.name === jobName && res !== null) {
        if (res.status === 204) {
          this.snackBarService.showMessage(`Successfully updated '${jobName}' with new status '${newStatus}' `, SnackBarStatus.Success);
          return {...job, status: newStatus};
        } else {
          this.snackBarService.showMessage(`Get '${res.status}' response when updating job '${jobName} status to '${newStatus}'.`, SnackBarStatus.Warning);
          return job;
        }
      } else {
        return job;
      }
    };
  }

  private handleError(error: HttpErrorResponse) {
    if (error.status === 0) {
      // A client-side or network error occurred. Handle it accordingly.
      console.error('An error occurred:', error.error);
    } else {
      // The backend returned an unsuccessful response code.
      // The response body may contain clues as to what went wrong.
      console.error(
        `Backend returned code ${error.status}, body was: `, error.error);
    }
    // Return an observable with a user-facing error message.
    return throwError(() => new Error('Something bad happened; please try again later.'));
  }
}

