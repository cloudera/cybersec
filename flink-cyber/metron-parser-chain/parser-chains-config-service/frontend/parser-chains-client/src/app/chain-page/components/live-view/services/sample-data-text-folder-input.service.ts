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
import {from, Observable} from 'rxjs';

import {EntryParsingResultModel} from '../models/live-view.model';
import {SampleDataInternalModel, SampleDataModel, SampleDataType} from '../models/sample-data.model';
import {LiveViewService} from "./live-view.service";
import {concatAll, map, reduce} from "rxjs/operators";
import {Store} from "@ngrx/store";
import {ChainListPageState, getSelectedPipeline} from "../../../../chain-list-page/chain-list-page.reducers";

@Injectable({
  providedIn: 'root'
})
export class SampleDataTextFolderInputService {

  static readonly SAMPLE_FOLDER_PARSER_URL = '/api/v1/parserconfig/tests/samples/';

  constructor(
    private _http: HttpClient,
    private _store$: Store<ChainListPageState>,
    private _liveViewService: LiveViewService,
  ) {
  }

  runTests(sampleDataList: SampleDataInternalModel[], chainConfig: unknown): Observable<Map<number, [SampleDataInternalModel, EntryParsingResultModel[]]>> {
    const resultList: Observable<{
      id: number,
      sample: SampleDataInternalModel,
      results: EntryParsingResultModel[]
    }>[] = []
    this._store$.select(getSelectedPipeline).subscribe(
      selectedPipeline => {
        sampleDataList.forEach(value => {
          const sample: SampleDataModel = {
            source: value.source,
            type: SampleDataType.MANUAL
          }
          const postObservable =
            this._liveViewService.execute(sample, chainConfig, selectedPipeline)
              .pipe(map(res => {
                return {
                  id: value.id,
                  sample: value,
                  results: res.results
                }
              }))
          resultList.push(postObservable)
        })
      }
    )
    return from(resultList).pipe(concatAll(), reduce((acc, value) => {
      return acc.set(value.id, [value.sample, value.results])
    }, new Map<number, [SampleDataInternalModel, EntryParsingResultModel[]]>()))
  }

  fetchSamples(folderPath: string, chainId: string): Observable<SampleDataInternalModel[]> {
    return this._http.post<SampleDataInternalModel[]>(
      SampleDataTextFolderInputService.SAMPLE_FOLDER_PARSER_URL + chainId,
      {folderPath});
  }

  saveSamples(folderPath: string, chainId: string, sampleList: SampleDataInternalModel[]): Observable<SampleDataInternalModel[]> {
    return this._http.put<SampleDataInternalModel[]>(
      SampleDataTextFolderInputService.SAMPLE_FOLDER_PARSER_URL + chainId,
      {folderPath, sampleList}
    )
  }
}
