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
import { map } from 'rxjs/operators';

import { ParserModel } from '../chain-page/chain-page.models';
import {ClusterModel, PipelineModel} from "../chain-list-page/chain.model";
import {
  getFinalBaseUrl,
  getHttpParams
} from "../chain-list-page/chain-list-page.utils";

@Injectable({
  providedIn: 'root'
})
export class AddParserPageService {

  private readonly URL_PREFIX = '/api/v1';
  private readonly BASE_URL = '/parserconfig/';

  constructor(
    private http: HttpClient
  ) {}

  public add(chainId: string, parser: ParserModel, pipeline: PipelineModel = null, cluster: ClusterModel) {
    let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

    let httpParams: HttpParams = getHttpParams(pipeline, cluster);

    return this.http.post(url + `chains/${chainId}/parsers`, parser, {params: httpParams});
  }

  public getParserTypes(cluster: ClusterModel) {
    let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

    let httpParams: HttpParams = getHttpParams(null, cluster);

    return this.http.get<{ id: string, name: string }[]>(url + `parser-types`, {params: httpParams});
  }

  public getParsers(chainId: string, pipeline: PipelineModel = null, cluster: ClusterModel) {
    let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

    let httpParams: HttpParams = getHttpParams(pipeline, cluster);

    return this.http.get<ParserModel[]>(url + `chains/${chainId}/parsers`, {params: httpParams})
      .pipe(
        map((parsers: ParserModel[]) => {
          return parsers;
        })
      );
  }
}
