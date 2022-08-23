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
