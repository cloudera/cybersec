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

  onApply(source: string) {
    this.sampleDataChange.emit({
      type: this.sampleData.type,
      source
    });
  }

  uploadToForm(e) {
    const file = e.target.files[0];
    if (!file) {
      console.log('No file selected');
      return;
    }
    console.log('Reading file:', file.name);
    const reader = new FileReader();
    reader.onload = () => {
      const fileTypeError = this._checkFileType(file);
      if (fileTypeError) {
        return;
      }
      const fileContent = convertToString(reader.result);
      console.log('File content length:', fileContent.length);
      console.log('First 100 chars:', fileContent.substring(0, 100));
      
      // Update local state first
      this.sampleData.source = fileContent;
      console.log('Updated sampleData.source:', this.sampleData.source.substring(0, 100));
      
      // Emit to parent
      this.sampleDataChange.emit({
        type: this.sampleData.type,
        source: fileContent
      });
      console.log('Emitted sampleDataChange event');
    };
    reader.onerror = () => {
      console.error('Error reading file:', reader.error);
    };
    reader.readAsText(file);
    // Clear the input so the same file can be selected again
    e.target.value = '';
  }

  private _checkFileType(file: File) {
    if (!file) {
      return false;
    }
    const fileName = file.name;
    const dotIndex = fileName.lastIndexOf('.');
    const extension = dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : '';
    const fileExt = ['txt', 'csv'];
    const fileTypes = ['text/plain', 'text/csv'];

    const isValidExt = fileExt.includes(extension);
    const isValidType = fileTypes.some(type => file.type === type || (file.type && file.type.startsWith(type)));

    console.log('File name:', fileName, 'extension:', extension, 'type:', file.type, 'isValidExt:', isValidExt, 'isValidType:', isValidType);

    // Allow the file if it has a valid extension, regardless of type
    if (isValidExt) {
      return false;
    }
    
    // If extension is invalid but type is valid text type, still allow it
    if (isValidType) {
      return false;
    }
    
    this._messageService.create('error', 'The file must be a .txt or .csv');
    return true;
  }

}
