import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'app-json-editor-popup',
  templateUrl: './json-editor-popup.component.html',
  styleUrls: ['./json-editor-popup.component.scss']
})
export class JsonEditorPopupComponent {

  constructor(public dialogRef: MatDialogRef<JsonEditorPopupComponent>,
              @Inject(MAT_DIALOG_DATA) public json: unknown) {  }

  closeDialog() {
    this.dialogRef.close();
  }

}
