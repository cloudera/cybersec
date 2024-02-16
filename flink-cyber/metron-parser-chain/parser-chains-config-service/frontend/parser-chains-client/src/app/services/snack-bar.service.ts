import {Injectable} from '@angular/core';
import {MatSnackBar,} from '@angular/material/snack-bar';


export enum SnackBarStatus {
  Success,
  Fail,
  Warning
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
  private messageQueue: SnackBarMessage[] = [];

  constructor(private snackBar: MatSnackBar) {
  }

  showMessage(
    message: string,
    status: SnackBarStatus = SnackBarStatus.Fail,
    action: string = 'Close',
    duration: number = 3000
  ): void {
    if (!this.snackBar._openedSnackBarRef && this.messageQueue.length === 0) {
      this.showSnackBar(message, status, action, duration);
    } else {
      this.messageQueue.push({message: message, status: status, action: action, duration: duration});
    }
  }

  private showSnackBar(
    message: string,
    status: SnackBarStatus = SnackBarStatus.Fail,
    action: string = 'Close',
    duration: number = 3000
  ): void {
    this.snackBar
      .open(message, action, {
        duration: duration,
        panelClass: this.getClasses(status),
        verticalPosition: 'top',
        horizontalPosition: 'center'
      })
      .afterDismissed()
      .subscribe(() => {
        const mes: SnackBarMessage = this.messageQueue.shift();
        if (!!mes && this.messageQueue.length > 0) {
          this.showSnackBar(mes.message, mes.status, mes.action, mes.duration);
        }
      });
  }

  getClasses(status: SnackBarStatus): string[] {
    switch (status) {
      case SnackBarStatus.Success:
        return ['success-snackbar'];
      case SnackBarStatus.Fail:
        return ['fail-snackbar'];
      case SnackBarStatus.Warning:
        return ['warning-snackbar']
      default:
        return ['fail-snackbar'];
    }
  }
}
