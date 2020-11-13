import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { JobsRoutingModule } from './jobs-routing.module';
import { JobListComponent } from './job-list/job-list.component';

import { NzTableModule } from 'ng-zorro-antd/table';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [JobListComponent],
  imports: [
    CommonModule,
    SharedModule,
    JobsRoutingModule,
    NzTableModule,
    NzIconModule,
    NzButtonModule
  ]
})
export class JobsModule { }
