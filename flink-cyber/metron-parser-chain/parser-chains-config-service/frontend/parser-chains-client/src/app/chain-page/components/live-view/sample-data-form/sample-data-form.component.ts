import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd';

import { SampleDataModel } from '../models/sample-data.model';

@Component({
  selector: 'app-sample-data-form',
  templateUrl: './sample-data-form.component.html',
  styleUrls: ['./sample-data-form.component.scss']
})
export class SampleDataFormComponent {

  @Input() sampleData: SampleDataModel;
  @Output() sampleDataChange = new EventEmitter<SampleDataModel>();
  @ViewChild('sampleDataInput', { static: true }) sampleDataInput: ElementRef;

  constructor(private messageService: NzMessageService) {}

  onApply(sampleDataInput: string) {
    this.sampleDataChange.emit({
      ...this.sampleData,
      source: sampleDataInput
    });
  }

  uploadToForm(e) {
    const file = e.target.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      const fileTypeError = this.checkFileType(file);
      if (fileTypeError) {
        this.messageService.create('error', fileTypeError.message);
        return;
      }
      this.sampleDataInput.nativeElement.value = reader.result;
      this.onApply(this.sampleDataInput.nativeElement.value);
    };
    reader.readAsText(file);
  }

  private checkFileType(file: File) {
    if (!file) {
      return null;
    }
    const [, extension] = file.name.split('.');
    const fileExt = ['txt', 'csv'];
    const fileTypes = ['text/plain', 'text/csv'];

    if (!fileExt.find(ext => ext === extension.toLowerCase()) || !fileTypes.find(type => file.type === type)) {
      return {
        message: `The file must be a .txt or .csv`
      };
    }
    return null;
  }

}
