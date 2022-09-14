import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SampleDataType } from '../models/sample-data.model';

import { LiveViewService } from './live-view.service';

describe('LiveViewService', () => {
  let service: LiveViewService;
  let http: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
    });

    service = TestBed.get(LiveViewService);
    http = TestBed.get(HttpClient);
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
