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

import {ChainModel, ChainOperationalModel} from '../chain-list-page/chain.model';
import {getHttpParams} from "../shared/service.utils";

@Injectable({
  providedIn: 'root'
})
export class ChainListPageService {

  static readonly BASE_URL = '/api/v1/parserconfig/';

  constructor(
    private _http: HttpClient
  ) {
  }

  public createChain(chain: ChainOperationalModel, pipeline: string = null) {
    const httpParams: HttpParams = getHttpParams(pipeline);

    return this._http.post<ChainModel>(ChainListPageService.BASE_URL + 'chains', chain, {params: httpParams});
  }

  public getChains(pipeline: string = null, params = null) {
    const httpParams: HttpParams = getHttpParams(pipeline);

    return this._http.get<ChainModel[]>(ChainListPageService.BASE_URL + 'chains', {params: httpParams});
  }

  public deleteChain(chainId: string, pipeline: string = null) {
    const httpParams: HttpParams = getHttpParams(pipeline);

    return this._http.delete(ChainListPageService.BASE_URL + 'chains/' + chainId, {params: httpParams});
  }

  public getPipelines() {
    const httpParams: HttpParams = getHttpParams(null);

    return this._http.delete(ChainListPageService.BASE_URL + 'pipeline', {params: httpParams});
  }

}
