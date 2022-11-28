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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import {ChainModel, ChainOperationalModel} from '../chain-list-page/chain.model';

@Injectable({
    providedIn: 'root'
})
export class ChainListPageService {

    private readonly BASE_URL = '/api/v1/parserconfig/';

    constructor(
      private http: HttpClient
    ) {}

    public createChain(chain: ChainOperationalModel) {
        return this.http.post<ChainModel>('/api/v1/parserconfig/chains', chain);
    }

    public getChains(params = null) {
        return this.http.get<ChainModel[]>(this.BASE_URL + 'chains');
    }

    public deleteChain(chainId: string) {
        return this.http.delete(this.BASE_URL + 'chains/' + chainId);
    }

}
