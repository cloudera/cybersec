import {NgModule} from "@angular/core";
import {MultiButtonComponent} from "./multi-button.component";
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import {CommonModule} from "@angular/common";

@NgModule({
  declarations: [
    MultiButtonComponent
  ],
  imports: [
    MatButtonToggleModule,
    CommonModule,
  ],
  providers: [],
  exports: [MultiButtonComponent]
})
export class MultiButtonModule {
}
