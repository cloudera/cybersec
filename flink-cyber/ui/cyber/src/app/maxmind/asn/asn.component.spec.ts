import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AsnComponent } from './asn.component';

describe('AsnComponent', () => {
  let component: AsnComponent;
  let fixture: ComponentFixture<AsnComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AsnComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AsnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
