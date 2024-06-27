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

import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';

import {ChainListPageService} from '../services/chain-list-page.service';
import {ChainListPageComponent} from './chain-list-page.component';
import {AsyncPipe, NgComponentOutlet, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {SharedModule} from 'src/app/shared/share.module';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatTableModule} from '@angular/material/table';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {ChainDialogComponent} from 'src/app/chain-list-page/component/chain-dialog.component';
import {PipelineService} from 'src/app/services/pipeline.service';
import {of} from 'rxjs';
import {ChainModel} from 'src/app/chain-list-page/chain.model';

const mockPipeline = ['foo-pipeline1', 'foo-pipeline2'];
const mockChains: ChainModel[] = [{id: '1', name: 'test1'}, {id: '2', name: 'test2'}];

describe('PipelinesComponent', () => {
  let component: ChainListPageComponent;
  let fixture: ComponentFixture<ChainListPageComponent>;
  let pipelineService: jasmine.SpyObj<PipelineService>
  let chainListPageService: jasmine.SpyObj<ChainListPageService>

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        SharedModule,
        MatCardModule,
        MatDividerModule,
        MatTooltipModule,
        MatTableModule,
        MatDialogModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        NgIf,
        NgTemplateOutlet,
        NgComponentOutlet,
        AsyncPipe,
        MatSelectModule,
        FormsModule,
        NgForOf,
        MatAutocompleteModule,
        RouterTestingModule,
        NoopAnimationsModule,
      ],
      declarations: [ChainListPageComponent, ChainDialogComponent],
      providers: [
        {
          provide: PipelineService,
          useValue: jasmine.createSpyObj('PipelineService', ['getPipelines', 'createPipeline', 'renamePipeline', 'deletePipeline'])
        },
        {
          provide: ChainListPageService,
          useValue: jasmine.createSpyObj('ChainListPageService', ['createChain', 'getChains', 'deleteChain', 'getPipelines'])
        },
      ]
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {

    pipelineService = TestBed.inject(PipelineService) as jasmine.SpyObj<PipelineService>;
    pipelineService.getPipelines.and.returnValue(of(mockPipeline));

    chainListPageService = TestBed.inject(ChainListPageService) as jasmine.SpyObj<ChainListPageService>;
    chainListPageService.getChains.and.returnValue(of(mockChains));

    fixture = TestBed.createComponent(ChainListPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
