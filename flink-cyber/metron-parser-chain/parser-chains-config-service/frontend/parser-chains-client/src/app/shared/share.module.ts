import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SortPipe} from './pipes/sort.pipe';
import {MultiButtonComponent} from './components/multibutton/multi-button.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {ConfirmDeleteDialogComponent} from './components/confirm-delete-dialog/confirm-delete-dialog.component';
import {A11yModule} from '@angular/cdk/a11y';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {WithLoadingPipe} from 'src/app/shared/pipes/with-loading.pipe';
import {CustomListComponent} from 'src/app/shared/components/custom-list/custom-list.component';
import {
  CustomListActionDirective,
  CustomListChipsDirective,
  CustomListItemComponent,
  CustomListIconDirective,
  CustomListLineDirective,
  CustomListSubheaderDirective
} from 'src/app/shared/components/custom-list/custom-list-item/custom-list-item.component';
import {MatIconModule} from '@angular/material/icon';
import {MatChipsModule} from '@angular/material/chips';
import {MatTooltipModule} from '@angular/material/tooltip';
import {StyledChipsListComponent} from './components/styled-chips-list/styled-chips-list.component';
import {UploadComponent} from './components/upload/upload/upload.component';
import {FileUploadModule} from 'src/app/cluster/component/file-upload/file-upload.module';
import {UploadProgressComponent} from 'src/app/shared/components/upload/upload/progress/progress.component';
import {FormsModule} from '@angular/forms';
import {FilterPipe} from './pipes/filter.pipe';
import { ContainsPipe } from './pipes/contains.pipe';

@NgModule({
  imports: [CommonModule, MatButtonToggleModule, A11yModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule, MatProgressBarModule, MatIconModule, MatChipsModule, MatTooltipModule, FileUploadModule, FormsModule],
  declarations: [SortPipe, FilterPipe, WithLoadingPipe, MultiButtonComponent, ConfirmDeleteDialogComponent, CustomListComponent, CustomListItemComponent, CustomListIconDirective, CustomListActionDirective, CustomListLineDirective, CustomListSubheaderDirective, CustomListChipsDirective, StyledChipsListComponent, UploadComponent, UploadProgressComponent, ContainsPipe],
  exports: [SortPipe, FilterPipe, WithLoadingPipe, MultiButtonComponent, ConfirmDeleteDialogComponent, CustomListItemComponent, CustomListComponent, CustomListIconDirective, CustomListActionDirective, CustomListLineDirective, CustomListSubheaderDirective, CustomListChipsDirective, StyledChipsListComponent, UploadComponent, ContainsPipe]
})
export class SharedModule {
}
