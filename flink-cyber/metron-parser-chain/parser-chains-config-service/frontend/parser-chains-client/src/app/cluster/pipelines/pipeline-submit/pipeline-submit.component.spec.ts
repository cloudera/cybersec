import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {PipelineSubmitComponent} from './pipeline-submit.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
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
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatStepperModule} from '@angular/material/stepper';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {FileUploadModule} from 'src/app/cluster/component/file-upload/file-upload.module';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ClusterPipelineService} from 'src/app/services/cluster.pipeline.service';
import {of} from 'rxjs';
import {RouterTestingModule} from '@angular/router/testing';

describe('PipelineSubmitComponent', () => {
  let component: PipelineSubmitComponent;
  let fixture: ComponentFixture<PipelineSubmitComponent>;
  let clusterPipelineService: jasmine.SpyObj<ClusterPipelineService>;
  let router: Router;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        RouterModule,
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
        MatStepperModule,
        MatProgressBarModule,
        MatSlideToggleModule,
        FileUploadModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule
      ],
      declarations: [PipelineSubmitComponent],
      providers: [
        {
          provide: ClusterPipelineService,
          useValue: jasmine.createSpyObj('ClusterPipelineService', ['createEmptyPipeline', 'startAllPipelines', 'getAllPipelines', 'getPipelines'])
        }
      ]
    })
      .compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'getCurrentNavigation').and.returnValue({
      extras: {state: {data: {clusterId: 'test1', pipelineName: 'foo-pipe', branch: 'testbranch'}}},
      id: 0,
      initialUrl: null,
      extractedUrl: null,
      trigger: null,
      previousNavigation: null
    });


    fixture = TestBed.createComponent(PipelineSubmitComponent);
    component = fixture.componentInstance;
    clusterPipelineService = TestBed.inject(ClusterPipelineService) as jasmine.SpyObj<ClusterPipelineService>;
    clusterPipelineService.createEmptyPipeline = jasmine.createSpy('createEmptyPipeline').and.returnValue(of({}));
    fixture.detectChanges();
  }));

  it('should create', () => {

    expect(component).toBeTruthy();
  });
});
