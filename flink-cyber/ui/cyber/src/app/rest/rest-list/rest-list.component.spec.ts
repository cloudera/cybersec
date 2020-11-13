import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RestListComponent } from './rest-list.component';

describe('RestListComponent', () => {
  let component: RestListComponent;
  let fixture: ComponentFixture<RestListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RestListComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RestListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
