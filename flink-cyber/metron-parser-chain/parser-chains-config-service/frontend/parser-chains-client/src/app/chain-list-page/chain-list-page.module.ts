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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NzTableModule } from 'ng-zorro-antd/table'
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzModalModule} from "ng-zorro-antd/modal";

import { ChainListPageComponent } from './chain-list-page.component';
import { ChainListEffects } from './chain-list-page.effects';
import { reducer } from './chain-list-page.reducers';
import {NzCardModule} from "ng-zorro-antd/card";
import {NzDividerModule} from "ng-zorro-antd/divider";
import {NzToolTipModule} from "ng-zorro-antd/tooltip";
import {NzButtonModule} from "ng-zorro-antd/button";
import {NzPopconfirmModule} from "ng-zorro-antd/popconfirm";
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import {NzSelectModule} from "ng-zorro-antd/select";

@NgModule({
  declarations: [ ChainListPageComponent ],
    imports: [
        NzModalModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NzInputModule,
        NzTableModule,
        RouterModule,
        StoreModule.forFeature('chain-list-page', reducer),
        EffectsModule.forFeature([ChainListEffects]),
        NzCardModule,
        NzDividerModule,
        NzToolTipModule,
        NzButtonModule,
        NzPopconfirmModule,
        NzIconModule,
        NzFormModule,
        NzLayoutModule,
        NzSelectModule,
    ],
  providers: [
    NzMessageService,
  ],
  exports: [ ChainListPageComponent ]
})
export class ChainListPageModule { }
