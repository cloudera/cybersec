import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {SampleDataModel} from "../../models/sample-data.model";
import {NzMessageService} from "ng-zorro-antd/message";

@Component({
  selector: 'app-sample-data-text-input',
  templateUrl: './sample-data-text-input.component.html',
  styleUrls: ['./sample-data-text-input.component.scss']
})
export class SampleDataTextInputComponent {

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
        return;
      }
      this.sampleDataInput.nativeElement.value = reader.result;
      this.onApply(this.sampleDataInput.nativeElement.value);
    };
    reader.readAsText(file);
  }

  private checkFileType(file: File) {
    if (!file) {
      return false;
    }
    const [, extension] = file.name.split('.');
    const fileExt = ['txt', 'csv'];
    const fileTypes = ['text/plain', 'text/csv'];

    if (!fileExt.find(ext => ext === extension.toLowerCase()) || !fileTypes.find(type => file.type === type)) {
        this.messageService.create('error', 'The file must be a .txt or .csv');
        return true;
    }
    return false;
  }

}
