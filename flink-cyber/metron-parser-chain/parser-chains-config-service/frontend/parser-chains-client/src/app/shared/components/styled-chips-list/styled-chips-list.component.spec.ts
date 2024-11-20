import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import { StyledChipsListComponent } from './styled-chips-list.component';
import {MatChipsModule} from '@angular/material/chips';

describe('StyledChipsListComponent', () => {
  let component: StyledChipsListComponent;
  let fixture: ComponentFixture<StyledChipsListComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [ MatChipsModule],
      declarations: [ StyledChipsListComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StyledChipsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
