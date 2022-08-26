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
    ],
  providers: [
    NzMessageService,
  ],
  exports: [ ChainListPageComponent ]
})
export class ChainListPageModule { }
