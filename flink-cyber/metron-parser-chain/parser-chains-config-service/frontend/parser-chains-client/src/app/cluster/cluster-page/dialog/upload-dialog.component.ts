import {Component, Inject, ViewChild} from "@angular/core";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Subscription} from "rxjs";
import {FileUploadComponent} from "../../component/file-upload/file-upload.component";

@Component({
  selector: 'app-upload-dialog',
  templateUrl: './upload-dialog.component.html',
})
export class UploadDialogComponent {
  @ViewChild(FileUploadComponent) uploadComponent: FileUploadComponent;

  confirmButtonText: string = "Upload"
  cancelButtonText: string = "Close"
  targetUrl: string;
  disabledUpload: boolean = true;
  disabledClose: boolean = false

  constructor(
    @Inject(MAT_DIALOG_DATA) private data: UploadDialogModel,
    private dialogRef: MatDialogRef<UploadDialogComponent>) {
    if (data) {
      this.targetUrl = data.targetUrl;
      if (data.buttonText) {
        this.confirmButtonText = data.buttonText.confirmButtonText || this.confirmButtonText;
        this.cancelButtonText = data.buttonText.cancelButtonText || this.cancelButtonText;
      }
    }
  }

  onConfirmClick(): void {
    this.uploadComponent.uploadFiles();
  }

  onCloseClick() {
    this.uploadComponent.deleteAllFiles();
    this.dialogRef.close(true);
  }

  preparedFile($event: boolean) {
    console.log("preparedFile: " + $event);
    if ($event === true) {
      this.disabledUpload = false;
    }
    if ($event === false) {
      this.disabledUpload = true;
    }
  }

  inProgressFile($event: boolean) {
    console.log("inProgressFile: " + $event)
    if ($event === true) {
      this.disabledUpload = true;
      this.disabledClose = true;
    }
    if ($event === false) {
      this.disabledClose = false;
    }
  }
}

export class UploadDialogModel {
  targetUrl: string;
  buttonText: {
    confirmButtonText: string,
    cancelButtonText: string
  }
}
