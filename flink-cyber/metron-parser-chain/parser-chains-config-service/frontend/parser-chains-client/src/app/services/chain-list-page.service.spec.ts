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

import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NzModalModule } from 'ng-zorro-antd/modal';

import {
  ChainModel,
  ChainOperationalModel
} from '../chain-list-page/chain.model';
import { ChainListPageService } from './chain-list-page.service';

let mockBackend: HttpTestingController;
let injector: TestBed;
let chainListPageService: ChainListPageService;

const chainListMockResponse: ChainModel[] = [
  {
    id: 'id1',
    name: 'Chain 1'
  },
  {
    id: 'id2',
    name: 'Chain 2'
  }
];

describe('ChainListPageService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        NzModalModule,
        NoopAnimationsModule
      ]
    });
    injector = getTestBed();
    chainListPageService = injector.get(ChainListPageService);
    mockBackend = injector.get(HttpTestingController);
  });

  it('should be created', () => {
    const service: ChainListPageService = TestBed.get(ChainListPageService);
    expect(service).toBeTruthy();
  });

  describe('getChainList()', () => {
    it('should return an Observable<string[]>', () => {
      const responseMock: ChainModel[] = chainListMockResponse;
      let response;

      chainListPageService.getChains().subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
        expect(response).toEqual(responseMock);
      });

      const req = mockBackend.expectOne(`/api/v1/parserconfig/chains`);
      expect(req.request.method).toEqual('GET');
      req.flush(responseMock);
    });

    it('should throw an error if something went wrong.', () => {
      const service: ChainListPageService = TestBed.get(ChainListPageService);
      const error = new ErrorEvent('Uh-oh');
      service.getChains().subscribe(null, err => {
        expect(err.error.type).toBe('Uh-oh');
      });
      const req = mockBackend.expectOne('/api/v1/parserconfig/chains');
      req.error(error);
      mockBackend.verify();
    });
  });

  describe('deleteChain()', () => {
    it('should return an Observable<string[]>', () => {
      const responseMock: ChainModel[] = chainListMockResponse;
      let response;

      chainListPageService.deleteChain('id1').subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
      });

      const req = mockBackend.expectOne(`/api/v1/parserconfig/chains/id1`);
      expect(req.request.method).toEqual('DELETE');
      req.flush(responseMock);
    });

    it('should throw an error if something went wrong.', () => {
      const service: ChainListPageService = TestBed.get(ChainListPageService);
      const error = new ErrorEvent(':(');
      service.deleteChain('id1').subscribe(null, err => {
        expect(err.error.type).toBe(':(');
      });
      const req = mockBackend.expectOne('/api/v1/parserconfig/chains/id1');
      req.error(error);
      mockBackend.verify();
    });
  });

  describe('createChain()', () => {
    it('should return an Observable<string[]>', () => {
      const service: ChainListPageService = TestBed.get(ChainListPageService);
      const responseMock: ChainModel[] = chainListMockResponse;
      let response;
      const reqBody: ChainOperationalModel = { name: 'Chain 3' };
      service.createChain(reqBody).subscribe(r => {
        response = r;
        expect(response).toBeTruthy();
      });

      const req = mockBackend.expectOne(`/api/v1/parserconfig/chains`);
      expect(req.request.method).toEqual('POST');
      req.flush(responseMock);
    });

    it('should throw an error if something went wrong.', () => {
      const service: ChainListPageService = TestBed.get(ChainListPageService);
      const reqBody: ChainOperationalModel = { name: 'Chain 3' };
      const error = new ErrorEvent(':(');
      service.createChain(reqBody).subscribe(null, err => {
        expect(err.error.type).toBe(':(');
      });
      const req = mockBackend.expectOne('/api/v1/parserconfig/chains');
      req.error(error);
      mockBackend.verify();
    });
  });
});
