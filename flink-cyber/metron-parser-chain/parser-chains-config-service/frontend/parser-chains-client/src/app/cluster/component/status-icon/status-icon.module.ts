import {NgModule} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {NzMessageService} from "ng-zorro-antd/message";
import {StatusIconComponent} from "./status-icon.component";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {CommonModule} from "@angular/common";

@NgModule({
  declarations: [
    StatusIconComponent
  ],
  imports: [
    MatIconModule,
    MatProgressSpinnerModule,
    CommonModule,
  ],
  providers: [
    NzMessageService
  ],
  exports: [StatusIconComponent]
})
export class StatusIconModule {
}
