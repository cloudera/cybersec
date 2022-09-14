import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { EntryParsingResultModel, LiveViewRequestModel } from '../models/live-view.model';
import { SampleDataModel, SampleDataRequestModel } from '../models/sample-data.model';

@Injectable({
  providedIn: 'root'
})
export class LiveViewService {

  readonly SAMPLE_PARSER_URL = '/api/v1/parserconfig/tests';

  constructor(
    private http: HttpClient,
  ) { }

  execute(sampleData: SampleDataModel, chainConfig: {}): Observable<{ results: EntryParsingResultModel[]}> {
    const sampleDataRequest: SampleDataRequestModel = {
      ...sampleData,
      source: sampleData.source.trimEnd().split('\n')
    };
    return this.http.post<{ results: EntryParsingResultModel[]}>(
      this.SAMPLE_PARSER_URL,
      { sampleData: sampleDataRequest, chainConfig } as LiveViewRequestModel);
  }
}
