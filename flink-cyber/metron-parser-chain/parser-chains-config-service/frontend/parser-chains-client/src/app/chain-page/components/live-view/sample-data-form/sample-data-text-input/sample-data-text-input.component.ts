import {Component, EventEmitter, Input, Output} from '@angular/core';
import {SampleDataModel} from "../../models/sample-data.model";
import {NzMessageService} from "ng-zorro-antd/message";
import {convertToString} from "../../../../../shared/utils";

@Component({
  selector: 'app-sample-data-text-input',
  templateUrl: './sample-data-text-input.component.html',
  styleUrls: ['./sample-data-text-input.component.scss']
})
export class SampleDataTextInputComponent {

  @Input() sampleData: SampleDataModel;
  @Output() sampleDataChange = new EventEmitter<SampleDataModel>();

  constructor(private _messageService: NzMessageService) {}

  onApply(event: Event) {
    const source = (event.target as HTMLInputElement).value;
    this.sampleDataChange.emit({
      type: this.sampleData.type,
      source
    });
    return source;
  }

  uploadToForm(e) {
    const file = e.target.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      const fileTypeError = this._checkFileType(file);
      if (fileTypeError) {
        return;
      }
      this.sampleData.source = convertToString(reader.result);
      this.sampleDataChange.emit(this.sampleData);
    };
    reader.readAsText(file);
  }

  private _checkFileType(file: File) {
    if (!file) {
      return false;
    }
    const [, extension] = file.name.split('.');
    const fileExt = ['txt', 'csv'];
    const fileTypes = ['text/plain', 'text/csv'];

    if (!fileExt.find(ext => ext === extension.toLowerCase()) || !fileTypes.find(type => file.type === type)) {
        this._messageService.create('error', 'The file must be a .txt or .csv');
        return true;
    }
    return false;
  }

}
