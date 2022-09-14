import { registerLocaleData } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import en from '@angular/common/locales/en';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { EffectsModule } from '@ngrx/effects';
import { MetaReducer, StoreModule } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import {  NzLayoutModule } from 'ng-zorro-antd/layout';
import { en_US,  NZ_I18N } from 'ng-zorro-antd/i18n'
import { NzModalModule } from 'ng-zorro-antd/modal'
import { storeFreeze } from 'ngrx-store-freeze';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { environment } from '../environments/environment';

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

registerLocaleData(en);

export const metaReducers: MetaReducer<{}>[] = !environment.production
  ? [storeFreeze]
  : [];

@NgModule({
  declarations: [
    AppComponent,
    MainContainerComponent,
    PageNotFoundComponent
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
        ChainAddParserPageModule,
        MonacoEditorModule,
        NzMenuModule,
        NzIconModule,
    ],
  providers: [
    { provide: NZ_I18N, useValue: en_US },
    CanDeactivateComponent,
    MonacoEditorService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
