import {ComponentFixture, TestBed} from '@angular/core/testing';

import {PipelineStepperComponent} from './pipeline-stepper.component';
import {FormsModule} from '@angular/forms';
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

describe('PipelineStepperComponent', () => {
  let component: PipelineStepperComponent;
  let fixture: ComponentFixture<PipelineStepperComponent>;

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
        NoopAnimationsModule
      ],

      declarations: [ PipelineStepperComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PipelineStepperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
