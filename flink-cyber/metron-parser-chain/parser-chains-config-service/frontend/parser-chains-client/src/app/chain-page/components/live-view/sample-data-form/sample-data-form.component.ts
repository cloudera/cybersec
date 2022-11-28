/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';

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
