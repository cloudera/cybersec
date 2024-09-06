import {ComponentFixture, TestBed} from '@angular/core/testing';

import {OcsfObjectFormComponent} from './ocsf-object-form.component';

describe('OcsfObjectFormComponent', () => {
  let component: OcsfObjectFormComponent;
  let fixture: ComponentFixture<OcsfObjectFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [OcsfObjectFormComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(OcsfObjectFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
