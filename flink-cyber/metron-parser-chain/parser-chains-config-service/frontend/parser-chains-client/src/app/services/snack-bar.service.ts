import {Injectable} from '@angular/core';
import {MatSnackBar,} from '@angular/material/snack-bar';


export enum SnackBarStatus {
  SUCCESS,
  FAIL,
  WARNING
}

interface SnackBarMessage {
  message: string;
  status: SnackBarStatus;
  action: string;
  duration: number;

}

@Injectable({
  providedIn: 'root',
})
export class SnackbarService {
  private _messageQueue: SnackBarMessage[] = [];

  constructor(private _snackBar: MatSnackBar) {
  }

  showMessage(
    message: string,
    status: SnackBarStatus = SnackBarStatus.FAIL,
    action: string = 'Close',
    duration: number = 3000
  ): void {
    if (!this._snackBar._openedSnackBarRef && this._messageQueue.length === 0) {
      this._showSnackBar(message, status, action, duration);
    } else {
      this._messageQueue.push({message, status, action, duration});
    }
  }

  getClasses(status: SnackBarStatus): string[] {
    switch (status) {
      case SnackBarStatus.SUCCESS:
        return ['success-snackbar'];
      case SnackBarStatus.FAIL:
        return ['fail-snackbar'];
      case SnackBarStatus.WARNING:
        return ['warning-snackbar']
      default:
        return ['fail-snackbar'];
    }
  }

  private _showSnackBar(
    message: string,
    status: SnackBarStatus = SnackBarStatus.FAIL,
    action: string = 'Close',
    duration: number = 3000
  ): void {
    this._snackBar
      .open(message, action, {
        duration,
        panelClass: this.getClasses(status),
        verticalPosition: 'top',
        horizontalPosition: 'center'
      })
      .afterDismissed()
      .subscribe(() => {
        const mes: SnackBarMessage = this._messageQueue.shift();
        if (!!mes && this._messageQueue.length > 0) {
          this._showSnackBar(mes.message, mes.status, mes.action, mes.duration);
        }
      });
  }
}
