import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { StoreRouterConnectingModule } from '@ngrx/router-store';
import { EffectsModule } from '@ngrx/effects';
import { IconsProviderModule } from './icons-provider.module';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NZ_I18N } from 'ng-zorro-antd/i18n';
import { en_US } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import en from '@angular/common/locales/en';
import { StoreModule } from '@ngrx/store';
import { EntityDataModule } from '@ngrx/data';
import { entityConfig } from './entity-metadata';
import {ChainAddParserPageModule} from './chain-add-parser-page/chain-add-parser-page.module';
import {ChainListPageModule} from './chain-list-page/chain-list-page.module';
import { MonacoEditorModule } from '@dmlukichev/ngx-monaco-editor'

registerLocaleData(en);

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    StoreRouterConnectingModule.forRoot(),
    EffectsModule.forRoot([]),
    IconsProviderModule,
    NzLayoutModule,
    NzMenuModule,
    ChainAddParserPageModule,
    ChainListPageModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    StoreModule.forRoot({}, {}),
    EntityDataModule.forRoot(entityConfig),
    // MonacoEditorModule.forRoot( {
    //   onMonacoLoad() {
    //     monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
    //       validate: true,
    //       schemas: []
    //     });
    //   }
    // })
  ],
  providers: [{ provide: NZ_I18N, useValue: en_US }],
  bootstrap: [AppComponent]
})
export class AppModule { }
