import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { StoreModule } from '@ngrx/store';
import {  NzMessageServiceModule } from 'ng-zorro-antd/message';


import { ParsersComponent } from './parsers.component';
import { reducer } from './parsers.reducers';
import { ChainViewComponent } from './components/chain-view/chain-view.component';
import { MultiInputComponent } from './components/custom-form/components/multi-input/multi-input.component';
import { CustomFormComponent } from './components/custom-form/custom-form.component';
import { LiveViewModule } from './components/live-view/live-view.module';
import { ParserComposerComponent } from './components/parser-composer/parser-composer.component';
import { AdvancedEditorComponent } from './components/parser/advanced-editor/advanced-editor.component';
import { ParserComponent } from './components/parser/parser.component';
import { RouteComponent } from './components/route/route.component';
import { RouterComponent } from './components/router/router.component';
import {NzGridModule} from "ng-zorro-antd/grid";
import {NzInputModule} from "ng-zorro-antd/input";
import {NzButtonModule} from "ng-zorro-antd/button";
import {NzToolTipModule} from "ng-zorro-antd/tooltip";
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { MonacoEditorModule } from 'ngx-monaco-editor-v13';
import { NzCollapseModule } from 'ng-zorro-antd/collapse';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzIconModule } from 'ng-zorro-antd/icon'
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzBreadCrumbModule } from 'ng-zorro-antd/breadcrumb';
import { ParsersRoutingModule } from './parsers-routing.module';
import { EffectsModule } from '@ngrx/effects';
import { ParsersPageEffects } from './parsers.effects';

@NgModule({
  declarations: [
    ParsersComponent,
    ChainViewComponent,
    ParserComponent,
    RouterComponent,
    CustomFormComponent,
    ParserComposerComponent,
    RouteComponent,
    AdvancedEditorComponent,
    MultiInputComponent,
  ],
  entryComponents: [ ChainViewComponent ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ParsersRoutingModule,
        RouterModule,
        StoreModule.forFeature('chain-page', reducer),
        EffectsModule.forFeature([ ParsersPageEffects ]),
        LiveViewModule,
        NzGridModule,
        NzInputModule,
        NzButtonModule,
        NzToolTipModule,
        NzFormModule,
        NzSelectModule,
        MonacoEditorModule,
        NzCollapseModule,
        NzTabsModule,
        NzIconModule,
        NzCardModule,
        NzBreadCrumbModule,
    ],
  providers: [
    NzMessageServiceModule,
  ],
  exports: [ ParsersComponent, ChainViewComponent ]
})
export class ParsersModule { }
