import {ComponentFixture, TestBed} from '@angular/core/testing';

import {PipelineStepperComponent} from './pipeline-stepper.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
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
import {AsyncPipe, NgComponentOutlet, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {MatSelectModule} from '@angular/material/select';
import {MatStepperModule} from '@angular/material/stepper';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {PipelineService} from 'src/app/services/pipeline.service';
import {of} from 'rxjs';
import {ChainListPageService} from 'src/app/services/chain-list-page.service';
import {ChainModel} from 'src/app/chain-list-page/chain.model';
import {Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {MatTabsModule} from '@angular/material/tabs';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatChipsModule} from '@angular/material/chips';
import {MatAutocompleteModule} from '@angular/material/autocomplete';

const mockPipeline = ['foo-pipeline1', 'foo-pipeline2'];
const mockChains: ChainModel[] = [{id: '1', name: 'test1'}, {id: '2', name: 'test2'}];

describe('PipelineStepperComponent', () => {
  let component: PipelineStepperComponent;
  let fixture: ComponentFixture<PipelineStepperComponent>;
  let pipelineService: jasmine.SpyObj<PipelineService>;
  let chainListPageService: jasmine.SpyObj<ChainListPageService>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({      imports: [
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
        MatStepperModule,
        RouterTestingModule.withRoutes([]),
        MatTabsModule,
        ReactiveFormsModule,
        MatSlideToggleModule,
        MatChipsModule,
        MatAutocompleteModule,
        NoopAnimationsModule
      ],
      providers: [
        {
          provide: PipelineService,
          useValue: jasmine.createSpyObj('PipelineService', ['getPipelines', 'createPipeline', 'renamePipeline', 'deletePipeline'])
        },
        {
          provide: ChainListPageService,
          useValue: jasmine.createSpyObj('ChainListPageService', ['createChain', 'getChains', 'deleteChain', 'getPipelines'])
        },
      ],

      declarations: [ PipelineStepperComponent ]
    })
    .compileComponents();

    pipelineService = TestBed.inject(PipelineService) as jasmine.SpyObj<PipelineService>;
    pipelineService.getPipelines.and.returnValue(of(mockPipeline));
    chainListPageService = TestBed.inject(ChainListPageService) as jasmine.SpyObj<ChainListPageService>;
    chainListPageService.getChains.and.returnValue(of(mockChains));

    router = TestBed.inject(Router);
    spyOn(router, 'getCurrentNavigation').and.returnValue({
      extras: {state: {data: {clusterId: 'test1', pipelineName: 'foo-pipe', branch: 'testbranch'}}},
      id: 0,
      initialUrl: null,
      extractedUrl: null,
      trigger: null,
      previousNavigation: null
    })
    fixture = TestBed.createComponent(PipelineStepperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
