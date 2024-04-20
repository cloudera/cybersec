import {Component, Inject, ViewChild} from "@angular/core";
import {MAT_DIALOG_DATA} from "@angular/material/dialog";
import {FileUploadComponent} from "../../component/file-upload/file-upload.component";
import {DialogData} from "../cluster-page.component";
import {DialogRef} from "@angular/cdk/dialog";


@Component({
  selector: 'app-upload-dialog',
  templateUrl: './upload-dialog.component.html',
})
export class UploadDialogComponent {
  @ViewChild(FileUploadComponent) uploadComponent: FileUploadComponent;

  confirmButtonText = "Upload"
  cancelButtonText = "Close"
  targetUrl: string;
  disabledUpload = true;
  disabledClose = false

  constructor(
    public dialogRef: DialogRef,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
) {
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
    if ($event === true) {
      this.disabledUpload = false;
    }
    if ($event === false) {
      this.disabledUpload = true;
    }
  }

  inProgressFile($event: boolean) {
    if ($event === true) {
      this.disabledUpload = true;
      this.disabledClose = true;
    }
    if ($event === false) {
      this.disabledClose = false;
    }
  }
}



