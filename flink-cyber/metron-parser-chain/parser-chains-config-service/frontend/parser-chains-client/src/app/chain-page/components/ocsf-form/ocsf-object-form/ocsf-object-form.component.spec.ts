import {ComponentFixture, TestBed} from '@angular/core/testing';

import {OcsfObjectFormComponent} from './ocsf-object-form.component';
import {ReactiveFormsModule, UntypedFormBuilder} from "@angular/forms";

describe('OcsfObjectFormComponent', () => {
  let component: OcsfObjectFormComponent;
  let fixture: ComponentFixture<OcsfObjectFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [OcsfObjectFormComponent],
      imports: [
        ReactiveFormsModule,
      ],
    })
      .compileComponents();

    fixture = TestBed.createComponent(OcsfObjectFormComponent);
    component = fixture.componentInstance;

    const formBuilder = TestBed.inject(UntypedFormBuilder);
    component.parentFormGroup = formBuilder.group({
      _filePath: "",
      _sourceName: ""
    });
    component.currentObject = {
      name: "Base Event",
      caption: "",
      description: "",
      attributes: {}
    }

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
