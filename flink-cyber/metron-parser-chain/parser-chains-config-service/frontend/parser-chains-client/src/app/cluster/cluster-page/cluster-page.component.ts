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
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject, forkJoin, of, throwError} from 'rxjs';
import {catchError, map, switchMap} from 'rxjs/operators';
import {Job} from '../cluster-list-page/cluster-list-page.model';
import {ClusterService} from '../../services/cluster.service';
import {SelectionModel} from '@angular/cdk/collections';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {SnackbarService, SnackBarStatus} from '../../services/snack-bar.service';
import {MatDialog} from '@angular/material/dialog';
import {UploadDialogComponent} from './dialog/upload-dialog.component';

export type DialogData = {
  targetUrl: string;
  buttonText: {
    confirmButtonText: string,
    cancelButtonText: string
  }
}

@Component({
  selector: 'app-cluster',
  templateUrl: './cluster-page.component.html',
  styleUrls: ['./cluster-page.component.scss']
})
export class ClusterPageComponent {
  clusterSubject$ = new BehaviorSubject<void>(null);
  cluster$ = this._route.paramMap.pipe(
    map((params) => params.get('clusterId')),
    switchMap((clusterId) => this._clusterService.getCluster(clusterId)));

  displayedColumns: string[] = ['select', 'name', 'type', 'branch', 'pipeline', 'status', 'created'];
  selection = new SelectionModel<Job>(true, []);

  constructor(
    private _route: ActivatedRoute,
    private _clusterService: ClusterService,
    private _snackBarService: SnackbarService,
    private _dialog: MatDialog
  ) {
  }

  isAllSelected(jobs: Job[]) {
    return this.selection.selected.length === jobs.length;
  }

  masterToggle(jobs: Job[]) {
    if (this.isAllSelected(jobs)) {
      this.selection.clear();
    } else {
      this.selection.select(...jobs);
    }
  }

  changeJobStatus(action: 'start' | 'restart' | 'stop' | 'update_config', clusterId: string) {
    forkJoin(
      this.selection.selected.reduce((acc, job) =>
          ({
            ...acc,
            [job.jobName]:
              this._clusterService.sendJobCommand(clusterId, action,
                {
                  jobIdHex: job.jobIdString,
                  pipelineDir: job.jobPipeline,
                  branch: job.jobPipeline
                }).pipe(catchError(err => {
                this._snackBarService.showMessage(err.message, SnackBarStatus.FAIL);
                return of(null);
              }))
          }),
        {})
    ).subscribe(_ => {
        this.clusterSubject$.next();
      },
      (error) => {
        //handle your error here
        this._snackBarService.showMessage(`Unexpected error ${error.message}`, SnackBarStatus.FAIL);
      }, () => {
        //observable completes
        this.selection.clear();
      });
  }

  openUploadDialog(clusterId: string) {
    this.selection.selected.forEach(job => {
      this._dialog.open(UploadDialogComponent, {
        data: {
          targetUrl: `/api/v1/clusters/${clusterId}/jobs/config/${job.jobPipeline}/${job.jobIdString}`,
        },
        maxWidth: '70vw',
        maxHeight: '80vh',
        height: '50%',
        width: '70%',
      })
    });
  }

  private _updateJobStatus(jobName: string, res: HttpResponse<any>, newStatus: 'start' | 'restart' | 'stop' | 'update_config') {
    return job => {
      if (job.name === jobName && res !== null) {
        if (res.status === 204) {
          this._snackBarService.showMessage(`Successfully updated '${jobName}' with new status '${newStatus}' `, SnackBarStatus.SUCCESS);
          return {...job, status: newStatus};
        } else {
          this._snackBarService.showMessage(`Get '${res.status}' response when updating job '${jobName} status to '${newStatus}'.`, SnackBarStatus.WARNING);
          return job;
        }
      } else {
        return job;
      }
    };
  }

  private _handleError(error: HttpErrorResponse) {
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

