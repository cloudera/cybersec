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
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzPopoverModule } from 'ng-zorro-antd/popover';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTabsModule } from 'ng-zorro-antd/tabs';

import { LiveViewResultComponent } from './live-view-result/live-view-result.component';
import { LiveViewComponent } from './live-view.component';
import { LiveViewEffects } from './live-view.effects';
import { reducer } from './live-view.reducers';
import { ParserByParserComponent } from './parser-by-parser/parser-by-parser.component';
import { SampleDataFormComponent } from './sample-data-form/sample-data-form.component';
import { LiveViewService } from './services/live-view.service';
import { StackTraceComponent } from './stack-trace/stack-trace.component';
import { NzTimelineModule } from 'ng-zorro-antd/timeline';
import { NzResultModule } from 'ng-zorro-antd/result';
import {NzCheckboxModule} from "ng-zorro-antd/checkbox";
import {NzIconModule} from "ng-zorro-antd/icon";
import { SampleDataTextInputComponent } from './sample-data-form/sample-data-text-input/sample-data-text-input.component';
import { SampleDataTextFolderInputComponent } from './sample-data-form/sample-data-text-folder-input/sample-data-text-folder-input.component';
import {NzToolTipModule} from "ng-zorro-antd/tooltip";
import {NzStepsModule} from "ng-zorro-antd/steps";
import {NzTableModule} from "ng-zorro-antd/table";
import {
    SampleDataTextFolderInputModule
} from "./sample-data-form/sample-data-text-folder-input/sample-data-text-folder-input.module";
import {NzCollapseModule} from "ng-zorro-antd/collapse";
import { TextDiffViewComponent } from './text-diff-view/text-diff-view.component';
import {MonacoEditorModule} from '@materia-ui/ngx-monaco-editor';
import {DiffPopupComponent} from "./diff-popup/diff-popup.component";
import {DiffPopupModule} from "./diff-popup/diff-popup.module";

@NgModule({
  declarations: [
    LiveViewComponent,
    SampleDataFormComponent,
    LiveViewResultComponent,
    ParserByParserComponent,
    StackTraceComponent,
    SampleDataTextInputComponent,
    SampleDataTextFolderInputComponent,
    TextDiffViewComponent,
    DiffPopupComponent,
  ],
  imports: [
    NzModalModule,
    CommonModule,
    FormsModule,
    StoreModule.forFeature('live-view', reducer),
    EffectsModule.forFeature([LiveViewEffects]),
    SampleDataTextFolderInputModule,
    DiffPopupModule,
    NzTabsModule,
    NzFormModule,
    NzButtonModule,
    NzRadioModule,
    NzInputModule,
    NzSpinModule,
    NzSwitchModule,
    NzCardModule,
    NzPopoverModule,
    NzTimelineModule,
    NzResultModule,
    NzCheckboxModule,
    ReactiveFormsModule,
    NzIconModule,
    NzToolTipModule,
    NzStepsModule,
    NzTableModule,
    NzCollapseModule,
    MonacoEditorModule,
  ],
  providers: [
    LiveViewService
  ],
  exports: [ LiveViewComponent ]
})
export class LiveViewModule { }
