import {ComponentFixture, TestBed} from '@angular/core/testing';

import {OcsfFormComponent} from './ocsf-form.component';

describe('OcsfFormComponent', () => {
  let component: OcsfFormComponent;
  let fixture: ComponentFixture<OcsfFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [OcsfFormComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(OcsfFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
