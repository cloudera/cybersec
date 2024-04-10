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

import {registerLocaleData} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import en from '@angular/common/locales/en';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {EffectsModule} from '@ngrx/effects';
import {MetaReducer, StoreModule} from '@ngrx/store';
import {StoreDevtoolsModule} from '@ngrx/store-devtools';
import {NzLayoutModule} from 'ng-zorro-antd/layout';
import {en_US, NZ_I18N} from 'ng-zorro-antd/i18n'
import {NzModalModule} from 'ng-zorro-antd/modal'
import {storeFreeze} from 'ngrx-store-freeze';

import {environment} from '../environments/environment';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ChainAddParserPageModule } from './chain-add-parser-page/chain-add-parser-page.module';
import { ChainListPageModule } from './chain-list-page/chain-list-page.module';
import { ChainPageModule } from './chain-page/chain-page.module';
import { CanDeactivateComponent } from './misc/can-deactivate-component';
import { MainContainerComponent } from './misc/main/main.container';
import { ThemeSwitchModule } from './misc/theme-switch/theme-switch.module';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { MonacoEditorService } from './services/monaco-editor.service';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { JsonEditorPopupComponent } from './components/popup/json-editor-popup/json-editor-popup.component';
import {MatDialogModule} from "@angular/material/dialog";
import {MatButtonModule} from "@angular/material/button";
import {DragDropModule} from "@angular/cdk/drag-drop";
import {MonacoEditorModule} from "ngx-monaco-editor-v2";
import {ClusterListPageModule} from "./cluster/cluster-list-page/cluster-list-page.module";
import {ClusterPageModule} from "./cluster/cluster-page/cluster-page.module";

registerLocaleData(en);

export const metaReducers: MetaReducer<{}>[] = !environment.production
  ? [storeFreeze]
  : [];

@NgModule({
  declarations: [
    AppComponent,
    MainContainerComponent,
    PageNotFoundComponent,
    JsonEditorPopupComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    NzModalModule,
    NzLayoutModule,
    ReactiveFormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    StoreModule.forRoot({}, {metaReducers}),
    StoreDevtoolsModule.instrument(),
    EffectsModule.forRoot([]),
    ThemeSwitchModule,
    ChainListPageModule,
    ChainPageModule,
    ClusterListPageModule,
    ClusterPageModule,
    ChainAddParserPageModule,
    MonacoEditorModule.forRoot(),
    NzMenuModule,
    NzIconModule,
  MatDialogModule,
    MatButtonModule,
    DragDropModule,
  ],
  providers: [
    {provide: NZ_I18N, useValue: en_US},
    CanDeactivateComponent,
    MonacoEditorService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
