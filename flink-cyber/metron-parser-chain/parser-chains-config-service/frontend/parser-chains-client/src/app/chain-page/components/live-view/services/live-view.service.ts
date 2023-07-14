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

import {HttpClient, HttpParams} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { EntryParsingResultModel, LiveViewRequestModel } from '../models/live-view.model';
import { SampleDataModel, SampleDataRequestModel } from '../models/sample-data.model';
import {getFinalBaseUrl, getHttpParams} from "../../../../chain-list-page/chain-list-page.utils";
import {ClusterModel} from "../../../../chain-list-page/chain.model";

@Injectable({
  providedIn: 'root'
})
export class LiveViewService {

  private readonly URL_PREFIX = '/api/v1';
  private readonly BASE_URL = '/parserconfig/tests';

  constructor(
    private http: HttpClient,
  ) { }

  execute(sampleData: SampleDataModel, chainConfig: {}, cluster: ClusterModel): Observable<{ results: EntryParsingResultModel[]}> {
    let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

    let httpParams: HttpParams = getHttpParams(null, cluster);

    const sampleDataRequest: SampleDataRequestModel = {
      ...sampleData,
      source: sampleData.source.trimEnd().split('\n')
    };
    return this.http.post<{ results: EntryParsingResultModel[]}>(
      url,
      { sampleData: sampleDataRequest, chainConfig } as LiveViewRequestModel, {params: httpParams});
  }
}
