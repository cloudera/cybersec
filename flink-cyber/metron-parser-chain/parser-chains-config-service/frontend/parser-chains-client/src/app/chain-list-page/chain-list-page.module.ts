import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NgZorroAntdModule, NzInputModule, NzMessageService, NzTableModule } from 'ng-zorro-antd';
import { MonacoEditorModule } from 'ngx-monaco-editor';

import { ChainListPageComponent } from './chain-list-page.component';
import { ChainListEffects } from './chain-list-page.effects';
import { reducer } from './chain-list-page.reducers';

@NgModule({
  declarations: [ ChainListPageComponent ],
  imports: [
    NgZorroAntdModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NzInputModule,
    NzTableModule,
    RouterModule,
    StoreModule.forFeature('chain-list-page', reducer),
    EffectsModule.forFeature([ ChainListEffects ]),
    MonacoEditorModule,
  ],
  providers: [
    NzMessageService,
  ],
  exports: [ ChainListPageComponent ]
})
export class ChainListPageModule { }
