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
import {  NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { AutofocusDirective } from '../misc/autofocus.directive';

import { ChainPageComponent } from './chain-page.component';
import { ChainPageEffects } from './chain-page.effects';
import { reducer } from './chain-page.reducers';
import { ChainViewComponent } from './components/chain-view/chain-view.component';
import { MultiInputComponent } from './components/custom-form/components/multi-input/multi-input.component';
import { CustomFormComponent } from './components/custom-form/custom-form.component';
import { LiveViewModule } from './components/live-view/live-view.module';
import { ParserComposerComponent } from './components/parser-composer/parser-composer.component';
import { AdvancedEditorComponent } from './components/parser/advanced-editor/advanced-editor.component';
import { ParserComponent } from './components/parser/parser.component';
import { RouteComponent } from './components/route/route.component';
import { RouterComponent } from './components/router/router.component';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzCollapseModule } from 'ng-zorro-antd/collapse';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzPopoverModule } from 'ng-zorro-antd/popover';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzBreadCrumbModule } from 'ng-zorro-antd/breadcrumb';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';

@NgModule({
  declarations: [
    ChainPageComponent,
    ChainViewComponent,
    ParserComponent,
    RouterComponent,
    CustomFormComponent,
    ParserComposerComponent,
    RouteComponent,
    AdvancedEditorComponent,
    AutofocusDirective,
    MultiInputComponent,
  ],
  entryComponents: [ ChainViewComponent ],
    imports: [
        NzModalModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule,
        StoreModule.forFeature('chain-page', reducer),
        EffectsModule.forFeature([ChainPageEffects]),
        MonacoEditorModule,
        LiveViewModule,
        NzTabsModule,
        NzCollapseModule,
        NzGridModule,
        NzToolTipModule,
        NzButtonModule,
        NzInputModule,
        NzPopoverModule,
        NzCardModule,
        NzBreadCrumbModule,
        NzFormModule,
        NzIconModule,
        NzPopconfirmModule,
        NzSelectModule,
    ],
  providers: [
    NzMessageService,
  ],
  exports: [ ChainPageComponent, ChainViewComponent ]
})
export class ChainPageModule { }
