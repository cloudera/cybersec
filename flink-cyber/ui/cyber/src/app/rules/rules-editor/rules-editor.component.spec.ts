import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RulesEditorComponent } from './rules-editor.component';

describe('RulesEditorComponent', () => {
  let component: RulesEditorComponent;
  let fixture: ComponentFixture<RulesEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RulesEditorComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RulesEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
