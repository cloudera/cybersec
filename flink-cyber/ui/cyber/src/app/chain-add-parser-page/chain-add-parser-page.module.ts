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
