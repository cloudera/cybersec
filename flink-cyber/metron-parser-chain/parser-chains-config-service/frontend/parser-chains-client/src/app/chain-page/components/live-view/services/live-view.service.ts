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

import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {EntryParsingResultModel, LiveViewRequestModel} from '../models/live-view.model';
import {SampleDataModel, SampleDataRequestModel} from '../models/sample-data.model';

@Injectable({
  providedIn: 'root'
})
export class LiveViewService {

  static readonly BASE_URL = '/api/v1/parserconfig/tests';

  constructor(
    private _http: HttpClient,
  ) { }

  execute(sampleData: SampleDataModel, chainConfig: unknown): Observable<{ results: EntryParsingResultModel[]}> {
    const sampleDataRequest: SampleDataRequestModel = {
      ...sampleData,
      source: sampleData.source.trimEnd().split('\n')
    };
    return this._http.post<{ results: EntryParsingResultModel[]}>(
      LiveViewService.BASE_URL,
      { sampleData: sampleDataRequest, chainConfig } as LiveViewRequestModel);
  }
}
