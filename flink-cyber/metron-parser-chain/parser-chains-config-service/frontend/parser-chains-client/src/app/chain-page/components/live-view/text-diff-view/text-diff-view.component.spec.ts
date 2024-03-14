import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import { TextDiffViewComponent } from './text-diff-view.component';
import {MonacoEditorModule} from "@materia-ui/ngx-monaco-editor";
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";

describe('TextDiffViewComponent', () => {
  let component: TextDiffViewComponent;
  let fixture: ComponentFixture<TextDiffViewComponent>;

  beforeEach(waitForAsync( () => {
    TestBed.configureTestingModule({
      imports: [MonacoEditorModule],
      declarations: [ TextDiffViewComponent ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TextDiffViewComponent);
    component = fixture.componentInstance;
    component.originalModel = 'original';
    component.modifiedModel = 'modified';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
