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

import {ClusterModel, PipelineModel} from '../chain-list-page/chain.model';
import {Observable} from "rxjs";
import {select, Store} from "@ngrx/store";
import {ChainListPageState, getCurrentPipeline} from "../chain-list-page/chain-list-page.reducers";

@Injectable({
    providedIn: 'root'
})
export class ClusterService {

    private readonly BASE_URL = '/api/v1/pipeline';

    currentPipeline$: Observable<PipelineModel>;

    constructor(
        private http: HttpClient,
        private store: Store<ChainListPageState>
    ) {
        this.currentPipeline$ = store.pipe(select(getCurrentPipeline))
    }

    public getCurrentCluster(): ClusterModel {
        return null;
    }

}
