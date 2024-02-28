// generate unit test for multi-button component

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MultiButtonComponent } from './multi-button.component';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

describe('MultiButtonComponent', () => {
  let component: MultiButtonComponent;
  let fixture: ComponentFixture<MultiButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MultiButtonComponent],
      imports: [MatButtonToggleModule, NoopAnimationsModule] // Add other necessary modules here
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MultiButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have buttons as per @Input', () => {
    component.buttons = [ {label: "Test1", value: "Value1"}, {label: "Test2", value: "Value2"} ];
    expect(component.buttons.length).toBe(2);
    expect(component.buttons.map(button => button.label)).toEqual(['Test1', 'Test2']);
  });

  it('should have buttons as default', () => {
    expect(component.buttons.length).toBe(3);
    expect(component.buttons.map(button => button.label)).toEqual(['Archive', 'Git', 'Manual']);
  });


  it('should emit valueChange event on button click', () => {
    spyOn(component.valueChange, 'emit');

    const buttonToggleGroup = fixture.debugElement.query(By.css('mat-button-toggle-group'));
    const buttonToggles = buttonToggleGroup.queryAll(By.css('mat-button-toggle'));

    // Simulate user interaction after ensuring the component is fully initialized

    buttonToggles[0].nativeElement.querySelector('.mat-button-toggle-button').click();
    fixture.detectChanges();

    expect(component.valueChange.emit).toHaveBeenCalledWith(component.group.value);
  });

});

