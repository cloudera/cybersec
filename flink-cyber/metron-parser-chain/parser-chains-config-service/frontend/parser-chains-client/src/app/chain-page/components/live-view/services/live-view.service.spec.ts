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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import { SampleDataType } from '../models/sample-data.model';

import { LiveViewService } from './live-view.service';

describe('LiveViewService', () => {
  let service: LiveViewService;
  let http: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ]
    });
    service = TestBed.inject(LiveViewService);
    http = TestBed.inject(HttpClient);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call proper endpoint with params', () => {
    spyOn(http, 'post');

    service.execute(
      { type: SampleDataType.MANUAL, source: 'test sample input' },
      { id: '456', name: 'gdf', parsers: [] }
    );

    expect(http.post).toHaveBeenCalledWith(
      service.SAMPLE_PARSER_URL,
      {
        sampleData: { type: SampleDataType.MANUAL, source: ['test sample input'] },
        chainConfig: { id: '456', name: 'gdf', parsers: [] }
      }
    );
  });
});
