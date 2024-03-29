import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import { TextDiffViewComponent } from './text-diff-view.component';
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";

describe('TextDiffViewComponent', () => {
  let component: TextDiffViewComponent;
  let fixture: ComponentFixture<TextDiffViewComponent>;

  beforeEach(waitForAsync( () => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [ TextDiffViewComponent ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TextDiffViewComponent);
    component = fixture.componentInstance;
    component.originalModelJson = 'original';
    component.modifiedModelJson = 'modified';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
