import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NgZorroAntdModule } from 'ng-zorro-antd';

import { AddParserEffects } from './chain-add-parser-page.effects';

import { ChainAddParserPageComponent } from './chain-add-parser-page.component';
import { reducer } from './chain-add-parser-page.reducers';

@NgModule({
  declarations: [ChainAddParserPageComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NgZorroAntdModule,
    StoreModule.forFeature('chain-add-parser-page', reducer),
    EffectsModule.forFeature([ AddParserEffects ]),
  ]
})
export class ChainAddParserPageModule { }
