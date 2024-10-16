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
import {map} from 'rxjs/operators';

import {ParserModel} from '../chain-page/chain-page.models';
import {getHttpParams} from "../shared/service.utils";

@Injectable({
  providedIn: 'root'
})
export class AddParserPageService {

  static readonly BASE_URL = '/api/v1/parserconfig/';

  constructor(
    private _http: HttpClient
  ) {}

  public add(chainId: string, parser: ParserModel, pipeline: string = null) {
    const httpParams: HttpParams = getHttpParams(pipeline);

    return this._http.post(AddParserPageService.BASE_URL + `chains/${chainId}/parsers`, parser, {params: httpParams});
  }

  public getParserTypes() {
    const httpParams: HttpParams = getHttpParams(null);

    return this._http.get<{ id: string, name: string }[]>(AddParserPageService.BASE_URL + `parser-types`, {params: httpParams});
  }

  public getParsers(chainId: string, pipeline: string = null) {
    const httpParams: HttpParams = getHttpParams(pipeline);

    return this._http.get<ParserModel[]>(AddParserPageService.BASE_URL + `chains/${chainId}/parsers`, {params: httpParams})
      .pipe(
        map((parsers: ParserModel[]) => {
          return parsers;
        })
      );
  }
}
