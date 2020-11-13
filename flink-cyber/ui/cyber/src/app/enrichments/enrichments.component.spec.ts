import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnrichmentsComponent } from './enrichments.component';

describe('EnrichmentsComponent', () => {
  let component: EnrichmentsComponent;
  let fixture: ComponentFixture<EnrichmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnrichmentsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnrichmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
