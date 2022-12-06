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
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NzMessageServiceModule } from 'ng-zorro-antd/message';

import { AddParserEffects } from './chain-add-parser-page.effects';

import { ChainAddParserPageComponent } from './chain-add-parser-page.component';
import { reducer } from './chain-add-parser-page.reducers';
import {NzCardModule} from "ng-zorro-antd/card";
import {NzGridModule} from "ng-zorro-antd/grid";
import {NzSelectModule} from "ng-zorro-antd/select";
import {NzFormModule} from "ng-zorro-antd/form";
import {NzInputModule} from "ng-zorro-antd/input";
import {NzButtonModule} from "ng-zorro-antd/button";

@NgModule({
  declarations: [ChainAddParserPageComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NzMessageServiceModule,
    StoreModule.forFeature('chain-add-parser-page', reducer),
    EffectsModule.forFeature([AddParserEffects]),
    NzCardModule,
    NzGridModule,
    NzSelectModule,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
  ]
})
export class ChainAddParserPageModule { }
