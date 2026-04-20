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
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {EntryParsingResultModel, LiveViewRequestModel} from '../models/live-view.model';
import {SampleDataModel, SampleDataRequestModel, SampleDataType} from '../models/sample-data.model';
import {getHttpParams} from "../../../../shared/service.utils";

@Injectable({
  providedIn: 'root'
})
export class LiveViewService {

  static readonly BASE_URL = '/api/v1/parserconfig/tests';

  constructor(
    private _http: HttpClient,
  ) {
  }

  execute(sampleData: SampleDataModel, chainConfig: unknown, pipeline: string = null): Observable<{
    results: EntryParsingResultModel[]
  }> {
    const httpParams: HttpParams = getHttpParams(pipeline);

    // Handle Avro binary data differently
    if (sampleData.type === SampleDataType.AVRO && sampleData.sourceBinary) {
      const sampleDataRequest: SampleDataRequestModel = {
        type: sampleData.type,
        source: [],
        sourceBinary: this.uint8ArrayToMatrix(sampleData.sourceBinary)
      };
      return this._http.post<{ results: EntryParsingResultModel[] }>(
        LiveViewService.BASE_URL,
        {sampleData: sampleDataRequest, chainConfig} as LiveViewRequestModel, {params: httpParams});
    }

    // Standard text-based input
    const sampleDataRequest: SampleDataRequestModel = {
      ...sampleData,
      source: sampleData.source.trimEnd().split('\n')
    };
    return this._http.post<{ results: EntryParsingResultModel[] }>(
      LiveViewService.BASE_URL,
      {sampleData: sampleDataRequest, chainConfig} as LiveViewRequestModel, {params: httpParams});
  }

  /**
   * Convert Uint8Array to number matrix for JSON serialization.
   * JSON doesn't support Uint8Array directly, so we convert to number[][]
   */
  private uint8ArrayToMatrix(uint8Array: Uint8Array): number[][] {
    const arr: number[] = [];
    for (let i = 0; i < uint8Array.length; i++) {
      arr.push(uint8Array[i]);
    }
    // Wrap in array to represent a list of binary records (single record for now)
    return [arr];
  }
}
