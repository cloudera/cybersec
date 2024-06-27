import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Component, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {SnackbarService, SnackBarStatus} from 'src/app/services/snack-bar.service';
import {TypedFormControls, uniqueAsyncValidator, uniqueValidator} from 'src/app/shared/utils';
import {DialogData, DialogForm} from 'src/app/chain-list-page/chain.model';

@Component({
  selector: 'app-chain-dialog',
  templateUrl: './chain-dialog.component.html',
  styleUrls: ['./chain-dialog.component.scss'],
  animations: []
})
export class ChainDialogComponent {
  dialogForm = new FormGroup<TypedFormControls<DialogForm>>({
    name: new FormControl(this.currentValue,
      this.validator,
      this.asyncValidator
    )
  });

  private _snackBarService = inject(SnackbarService);
  private _data = inject(MAT_DIALOG_DATA) as DialogData<any>;
  private _dialogRef = inject(MatDialogRef);



  get validator() {
    if (this._data.existValues instanceof Observable) {
      return [Validators.required, Validators.minLength(3)]
    }
    return [Validators.required, Validators.minLength(3),  uniqueValidator(this._data.existValues, this._data.columnUniqueKey)]
  }

  get asyncValidator() {
    if (this._data.existValues instanceof Observable) {
      return [uniqueAsyncValidator(this._data.existValues, this._data.columnUniqueKey)]
    }
    return []
  }

  get type(){
    return this._data.type;
  }

  get name() {
    return this._data.name;
  }

  get currentValue() {
    return this._data.currentValue ? this._data.currentValue : '';
  }
  get formName() {
    return this.dialogForm.controls.name;
  }

  get errorMessage() {
    if (this.formName.errors) {
      if (this.formName.errors.required) {
        return `${this.name} is required`;
      }
      if (this.formName.errors.minlength) {
        return `${this.name} name should have minimum 3 characters`;
      }
      if (this.formName.errors.uniqueValue) {
        return `${this.name} name is not unique`;
      }
      if (this.formName.errors.httpError) {
        return 'Get error response from server';
      }
    }
    return 'Unrecognized error';
  }

  submit() {
    this._data.action(this.dialogForm.getRawValue()).subscribe(
      (value) => {
        this._snackBarService.showMessage(`Successfully ${this.type} item`, SnackBarStatus.SUCCESS);
        this._dialogRef.close({response: value, form: this.dialogForm.getRawValue()});
      },
      (error) => {
        this.formName.setErrors({httpError: true});
        this._snackBarService.showMessage(error.message);
      }
    )
  }

  disabled() {
    if (this.type === 'create') {
      return this.dialogForm.invalid;
    }
    return this.dialogForm.invalid || !this.dialogForm.dirty;
  }
}
