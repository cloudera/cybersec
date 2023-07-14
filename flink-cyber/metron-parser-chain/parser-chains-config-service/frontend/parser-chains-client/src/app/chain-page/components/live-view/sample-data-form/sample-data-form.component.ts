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

import {SampleDataInternalModel, SampleDataModel} from '../models/sample-data.model';
import {EntryParsingResultModel} from "../models/live-view.model";

@Component({
  selector: 'app-sample-data-form',
  templateUrl: './sample-data-form.component.html',
  styleUrls: ['./sample-data-form.component.scss']
})
export class SampleDataFormComponent {

  @Input() sampleData: SampleDataModel;
  @Input() sampleDataFolder: string;
  @Input() chainConfig: {};
  @Output() sampleDataChange = new EventEmitter<SampleDataModel>();
  @Output() sampleDataForceChange = new EventEmitter<SampleDataInternalModel[]>();
  @ViewChild('sampleDataInput', { static: true }) sampleDataInput: ElementRef;

}
