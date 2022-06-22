import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NgZorroAntdModule, NzMessageService } from 'ng-zorro-antd';
import { MonacoEditorModule } from 'ngx-monaco-editor';

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
    NgZorroAntdModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    StoreModule.forFeature('chain-page', reducer),
    EffectsModule.forFeature([ ChainPageEffects ]),
    MonacoEditorModule,
    LiveViewModule,
  ],
  providers: [
    NzMessageService,
  ],
  exports: [ ChainPageComponent, ChainViewComponent ]
})
export class ChainPageModule { }
