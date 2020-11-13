import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HbaseComponent } from './hbase.component';

describe('HbaseComponent', () => {
  let component: HbaseComponent;
  let fixture: ComponentFixture<HbaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ HbaseComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HbaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
