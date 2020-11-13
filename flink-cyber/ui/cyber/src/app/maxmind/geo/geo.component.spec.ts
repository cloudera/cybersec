import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GeoComponent } from './geo.component';

describe('GeoComponent', () => {
  let component: GeoComponent;
  let fixture: ComponentFixture<GeoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GeoComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GeoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
