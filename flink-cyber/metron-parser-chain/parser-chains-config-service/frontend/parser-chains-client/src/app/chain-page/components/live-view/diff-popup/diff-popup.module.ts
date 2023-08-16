import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {StoreModule} from "@ngrx/store";
import {reducer} from "./diff-popup.reducers";



@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    StoreModule.forFeature('diff-popup', reducer),
  ]
})
export class DiffPopupModule { }
