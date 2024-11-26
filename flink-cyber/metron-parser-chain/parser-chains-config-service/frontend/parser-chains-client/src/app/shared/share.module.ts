import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SortPipe} from './pipes/sort.pipe';
import {MultiButtonComponent} from './components/multibutton/multi-button.component';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import { ConfirmDeleteDialogComponent } from './components/confirm-delete-dialog/confirm-delete-dialog.component';
import {A11yModule} from '@angular/cdk/a11y';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatProgressBarModule} from '@angular/material/progress-bar';

@NgModule({
  imports: [CommonModule, MatButtonToggleModule, A11yModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule, MatProgressBarModule],
  declarations: [ SortPipe, MultiButtonComponent, ConfirmDeleteDialogComponent ],
  exports:      [ SortPipe, MultiButtonComponent, ConfirmDeleteDialogComponent ]
})
export class SharedModule { }
