import {NgModule} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {NzMessageService} from "ng-zorro-antd/message";
import {FileUploadComponent} from "./file-upload.component";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {CommonModule} from "@angular/common";
import {MatProgressBarModule} from "@angular/material/progress-bar";
import {MatButtonModule} from "@angular/material/button";
import {DragDropDirective} from "../../../misc/drag-drop.directives";
import {ProgressComponent} from "./progress/progress.component";

@NgModule({
  declarations: [
    FileUploadComponent,
    DragDropDirective,
    ProgressComponent
  ],
  imports: [
    MatIconModule,
    MatProgressSpinnerModule,
    CommonModule,
    MatProgressBarModule,
    MatButtonModule,
  ],
  providers: [
    NzMessageService
  ],
  exports: [FileUploadComponent]
})
export class FileUploadModule {
}
