import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {PipelineCreateComponent} from './pipeline-create.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
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
import {of} from 'rxjs';
import {ClusterService} from 'src/app/services/cluster.service';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';

describe('PipelineCreateComponent', () => {
  let component: PipelineCreateComponent;
  let fixture: ComponentFixture<PipelineCreateComponent>;
  let clusterService:  jasmine.SpyObj<ClusterService>;

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
        NoopAnimationsModule
      ],
      declarations: [PipelineCreateComponent],
      providers: [
        {provide: ActivatedRoute, useValue: {params: of({id: '123'}), queryParams: of({pipeline: 'test-pipeline'})}},
        {provide: Router, useValue: {events: of({})}},
        {provide: ClusterService, useValue: jasmine.createSpyObj('ClusterService', ['getClusters', 'getCluster', 'sendJobCommand'])
  }
      ]
    }).compileComponents();

    clusterService = TestBed.inject(ClusterService) as jasmine.SpyObj<ClusterService>;
    clusterService.getClusters.and.returnValue(of([]));
    fixture = TestBed.createComponent(PipelineCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
