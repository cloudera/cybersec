import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TextDiffViewComponent } from './text-diff-view.component';

describe('TextDiffViewComponent', () => {
  let component: TextDiffViewComponent;
  let fixture: ComponentFixture<TextDiffViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TextDiffViewComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TextDiffViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
