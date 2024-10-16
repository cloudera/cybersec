import {NgModule} from "@angular/core";
import {ClusterListPageComponent} from "./cluster-list-page.component";
import {MatTableModule} from "@angular/material/table";
import {StatusIconModule} from "../component/status-icon/status-icon.module";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {FormsModule} from "@angular/forms";
import {MatRadioModule} from "@angular/material/radio";
import {MatCardModule} from "@angular/material/card";
import {CommonModule} from "@angular/common";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {ClusterPageModule} from "../cluster-page/cluster-page.module";
import {RouterLink} from '@angular/router';

@NgModule({
  declarations: [
    ClusterListPageComponent,
  ],
  imports: [
    MatTableModule,
    ClusterPageModule,
    StatusIconModule,
    MatCheckboxModule,
    FormsModule,
    MatRadioModule,
    MatCardModule,
    CommonModule,
    MatProgressSpinnerModule,
    RouterLink,
  ],
  providers: [],
  exports: [ClusterListPageComponent]
})
export class ClusterListPageModule {
}
