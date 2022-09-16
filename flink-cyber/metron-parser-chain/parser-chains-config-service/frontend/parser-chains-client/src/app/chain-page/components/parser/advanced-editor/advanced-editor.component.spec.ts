import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { AdvancedEditorComponent } from './advanced-editor.component';

describe('AdvancedEditorComponent', () => {
  let component: AdvancedEditorComponent;
  let fixture: ComponentFixture<AdvancedEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MonacoEditorModule,
      ],
      declarations: [ AdvancedEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch event when value changes', () => {
    const mockListener = jasmine.createSpy('mockListener');
    component.configChanged.subscribe(mockListener);

    component.onChange('{ "someField": "some value" }');

    expect(mockListener).toHaveBeenCalledWith({ value: JSON.parse('{ "someField": "some value" }') });
  });

  it('should ignore content if not valid json', () => {
    const mockListener = jasmine.createSpy('mockListener');
    component.configChanged.subscribe(mockListener);

    component.onChange('not a json, but sure it can be typed in to the editor');

    expect(mockListener).not.toHaveBeenCalledWith();
  });

  it('should not emit event if new value equals with the original', () => {
    const mockListener = jasmine.createSpy('mockListener');
    component.config = { initialField: 'initial value' };
    component.configChanged.subscribe(mockListener);

    component.onChange('{ initialField: "initial value" }');

    expect(mockListener).not.toHaveBeenCalledWith();
  });

});
