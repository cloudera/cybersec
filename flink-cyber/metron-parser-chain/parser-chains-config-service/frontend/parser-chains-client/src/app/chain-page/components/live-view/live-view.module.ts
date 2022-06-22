import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { NgZorroAntdModule } from 'ng-zorro-antd';
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

@NgModule({
  declarations: [
    LiveViewComponent,
    SampleDataFormComponent,
    LiveViewResultComponent,
    ParserByParserComponent,
    StackTraceComponent,
  ],
  imports: [
    NgZorroAntdModule,
    CommonModule,
    FormsModule,
    StoreModule.forFeature('live-view', reducer),
    EffectsModule.forFeature([ LiveViewEffects ]),
    NzTabsModule,
    NzFormModule,
    NzButtonModule,
    NzRadioModule,
    NzInputModule,
    NzSpinModule,
    NzSwitchModule,
    NzCardModule,
    NzPopoverModule,
  ],
  providers: [
    LiveViewService
  ],
  exports: [ LiveViewComponent ]
})
export class LiveViewModule { }
