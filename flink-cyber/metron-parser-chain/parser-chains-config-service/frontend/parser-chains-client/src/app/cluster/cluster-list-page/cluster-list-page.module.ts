import {NgModule} from "@angular/core";
import {ClusterListPageComponent} from "./cluster-list-page.component";
import {MatTableModule} from "@angular/material/table";
import {StatusIconModule} from "../component/status-icon/status-icon.module";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormsModule} from "@angular/forms";
import {MatRadioModule} from "@angular/material/radio";
import {MatCardModule} from "@angular/material/card";

@NgModule({
  declarations: [
    ClusterListPageComponent
  ],
  imports: [
    MatTableModule,
    StatusIconModule,
    MatCheckboxModule,
    FormsModule,
    MatRadioModule,
    MatCardModule,
  ],
  providers: [
  ],
  exports: [ClusterListPageComponent]
})
export class ClusterListPageModule {
}
