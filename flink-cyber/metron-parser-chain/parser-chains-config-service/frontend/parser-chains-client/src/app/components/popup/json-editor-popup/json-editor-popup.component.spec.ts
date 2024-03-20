import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {JsonEditorPopupComponent} from './json-editor-popup.component';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from "@angular/material/dialog";
import {MockComponent} from "ng-mocks";
import {AdvancedEditorComponent} from "../../../chain-page/components/parser/advanced-editor/advanced-editor.component";

describe('JsonEditorPopupComponent', () => {
  let component: JsonEditorPopupComponent;
  let fixture: ComponentFixture<JsonEditorPopupComponent>;
  let dialogRef: MatDialogRef<JsonEditorPopupComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [MatDialogModule],
      declarations: [JsonEditorPopupComponent, MockComponent(AdvancedEditorComponent)],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            json: {foo: 'bar'}
          }
        },
        {
          provide: MatDialogRef,
          useValue: jasmine.createSpyObj('MatDialogRef', ['closeDialog'])
        }
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(JsonEditorPopupComponent);
    component = fixture.componentInstance;
    dialogRef = TestBed.inject(MatDialogRef);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
