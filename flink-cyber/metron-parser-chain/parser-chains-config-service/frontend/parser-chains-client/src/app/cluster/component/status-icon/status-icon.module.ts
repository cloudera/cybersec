import {NgModule} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {NzMessageService} from "ng-zorro-antd/message";
import {StatusIconComponent} from "./status-icon.component";

@NgModule({
  declarations: [
    StatusIconComponent
  ],
  imports: [
    MatIconModule,
  ],
  providers: [
    NzMessageService
  ],
  exports: [StatusIconComponent]
})
export class StatusIconModule {
}
