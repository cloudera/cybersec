import { Component, OnInit } from '@angular/core';
import { ActionSpec } from 'src/app/shared/action-buttons/action-buttons.component';

export enum JobStatus {
  RUNNING, COMPLETE, FAILED, RESTARTING, STOPPED
}

export interface SqlJob {
  id: string;
  sql: string;
  start: Date;
  end: Date;
  lag: number;
  status: JobStatus;
  actions: string[];
}

export interface MainJob {
  name: string;
  start: Date;
  duration: number;
  eps: number;
  lag: number;
  savepoints: number;
  status: JobStatus;
  description: string;
  actions: string[];
}

@Component({
  selector: 'app-job-list',
  templateUrl: './job-list.component.html',
  styleUrls: ['./job-list.component.sass']
})
export class JobListComponent implements OnInit {
  expandSetMain = new Set<string>();
  expandSetSQL = new Set<string>();

  mainActions: ActionSpec[] = [
    { name: 'Start', colour: 'primary', tooltip: 'Start the job' },
    { name: 'Stop', colour: 'danger', tooltip: 'Stop the job', danger: true },
    { name: 'Configure', colour: 'secondary', tooltip: 'Edit configuration for this job' },
    { name: 'Restart', colour: 'primary', tooltip: 'Restart the job, preserving state' },
    { name: 'Details', colour: 'default', tooltip: 'Go to the flink dashboard for this job' }
  ];
  sqlActions: ActionSpec[] = [
    { name: 'Start', colour: 'primary', tooltip: 'Start the job' },
    { name: 'Stop', colour: 'danger', tooltip: 'Stop the job', danger: true }
  ];

  mainJobs: MainJob[] = [{
    name: 'Enrichment - Combined',
    start: new Date(),
    duration: 1234,
    eps: 1000,
    lag: 0,
    savepoints: 0,
    status: JobStatus.RUNNING,
    description: '',
    actions: ['Start', 'Stop', 'Configure']
  }];

  sqlJobs: SqlJob[] = [];

  onExpandMainChange(name: string, checked: boolean): void {
    if (checked) {
      this.expandSetMain.add(name);
    } else {
      this.expandSetMain.delete(name);
    }
  }
  onExpandSQLChange(id: string, checked: boolean): void {
    if (checked) {
      this.expandSetSQL.add(id);
    } else {
      this.expandSetSQL.delete(id);
    }
  }

  onAction(data: MainJob, action: ActionSpec): void {
    console.log(data, action);
  }
  onSqlAction(data: SqlJob, action: ActionSpec): void {
    console.log(data, action);
  }
  constructor() { }

  ngOnInit(): void {
  }

}
