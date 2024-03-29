import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JsonEditorPopupComponent } from './json-editor-popup.component';

describe('JsonEditorPopupComponent', () => {
  let component: JsonEditorPopupComponent;
  let fixture: ComponentFixture<JsonEditorPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ JsonEditorPopupComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(JsonEditorPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
