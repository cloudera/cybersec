import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {StoreModule} from "@ngrx/store";
import {EffectsModule} from "@ngrx/effects";
import {SampleDataTextFolderInputEffects} from "./sample-data-text-folder-input.effects";
import {reducer} from "./sample-data-text-folder-input.reducers";
import { SampleEditPopupComponent } from './sample-edit-popup/sample-edit-popup.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {NzFormModule} from "ng-zorro-antd/form";
import {NzModalModule} from "ng-zorro-antd/modal";
import {NzSelectModule} from "ng-zorro-antd/select";
import {NzCheckboxModule} from "ng-zorro-antd/checkbox";
import {NzInputModule} from "ng-zorro-antd/input";

@NgModule({
    declarations: [
        SampleEditPopupComponent
    ],
    exports: [
        SampleEditPopupComponent
    ],
    imports: [
        CommonModule,
        StoreModule.forFeature('sample-folder', reducer),
        EffectsModule.forFeature([SampleDataTextFolderInputEffects]),
        ReactiveFormsModule,
        NzFormModule,
        NzModalModule,
        NzSelectModule,
        FormsModule,
        NzCheckboxModule,
        NzInputModule,
    ]
})
export class SampleDataTextFolderInputModule { }
