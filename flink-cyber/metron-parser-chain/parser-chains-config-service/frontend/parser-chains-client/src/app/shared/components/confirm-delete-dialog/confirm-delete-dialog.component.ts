import {Component, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {SnackbarService, SnackBarStatus} from 'src/app/services/snack-bar.service';

export type DeleteDialogData = {
  action: () => Observable<void>;
}

@Component({
  selector: 'app-confirm-delete-dialog',
  templateUrl: './confirm-delete-dialog.component.html',
  styleUrls: ['./confirm-delete-dialog.component.scss']
})
export class ConfirmDeleteDialogComponent {
  private _data = inject(MAT_DIALOG_DATA) as DeleteDialogData;
  private _dialogRef = inject(MatDialogRef);
  private _snackBar = inject(SnackbarService);
  loading = false;

  delete() {
    this.loading = true;
    this._data.action().subscribe(
      result => {
        this.loading = false;
        this._snackBar.showMessage('Successfully deleted', SnackBarStatus.SUCCESS);
        this._dialogRef.close(result);
      },
      (error) => {
        this.loading = false;
        this._snackBar.showMessage('Failed to delete ' + error?.message);
      }
    );
  }
}
