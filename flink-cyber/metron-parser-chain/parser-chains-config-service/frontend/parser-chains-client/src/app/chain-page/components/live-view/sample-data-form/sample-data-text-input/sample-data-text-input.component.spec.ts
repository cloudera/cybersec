import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {SampleDataTextInputComponent} from './sample-data-text-input.component';
import {NzMessageService} from "ng-zorro-antd/message";
import {NzButtonModule} from "ng-zorro-antd/button";
import {NzFormModule} from "ng-zorro-antd/form";
import {SampleDataType} from "../../models/sample-data.model";
import {NzInputModule} from "ng-zorro-antd/input";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {findEl} from "src/app/shared/test/test-helper";

describe('SampleDataTextInputComponent', () => {
  let component: SampleDataTextInputComponent;
  let fixture: ComponentFixture<SampleDataTextInputComponent>;
  let messageService: NzMessageService;
  const mockFileReader = jasmine.createSpyObj('FileReader', ['readAsText']);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [SampleDataTextInputComponent],
      imports: [
        NzButtonModule,
        NzFormModule,
        NzInputModule,
        NoopAnimationsModule
      ],
      providers: [
        NzMessageService
      ],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataTextInputComponent);
    messageService = TestBed.inject(NzMessageService);
    component = fixture.componentInstance;
    component.sampleData = {type: SampleDataType.KAFKA, source: ''};
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit sampleData on submit', () => {
    spyOn(component.sampleDataChange, 'emit');
    const sampleInput = findEl(fixture, 'sample-input').nativeElement;
    sampleInput.value = 'foo-test';
    sampleInput.dispatchEvent(new Event('input'));
    expect(component.sampleDataChange.emit).toHaveBeenCalledWith({type: SampleDataType.KAFKA, source: 'foo-test'});
  });

  it('should emit sampleData on file upload', () => {
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(new File(['some text'], 'test.txt', {type: 'text/plain'}));
    spyOn(window, 'FileReader').and.returnValue(mockFileReader);
    spyOn(component.sampleDataChange, 'emit');
    mockFileReader.result = 'mock text';

    const fileInput = findEl(fixture, 'upload-input').nativeElement;
    fileInput.files = dataTransfer.files;
    fileInput.dispatchEvent(new Event('input'));

    // Execute the reader's onload manually and simulate the load event
    mockFileReader.onload();

    expect(mockFileReader.readAsText).toHaveBeenCalledWith(jasmine.any(File));
    expect(component.sampleDataChange.emit).toHaveBeenCalledWith({type: SampleDataType.KAFKA, source: 'mock text'});
  });

  it('should not emit sampleData on file upload if file type is not supported', () => {
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(new File(['wrong file content'], 'wrongImageFile.jpg', {type: 'image/jpeg'}));
    spyOn(component.sampleDataChange, 'emit');
    spyOn(messageService, 'create');
    spyOn(window, 'FileReader').and.returnValue(mockFileReader);

    const fileInput = findEl(fixture, 'upload-input').nativeElement;
    fileInput.files = dataTransfer.files;
    fileInput.dispatchEvent(new Event('input'));

    // Execute the reader's onload manually and simulate the load event
    mockFileReader.onload();

    expect(mockFileReader.readAsText).toHaveBeenCalledWith(jasmine.any(File));
    expect(component.sampleDataChange.emit).not.toHaveBeenCalled();
    expect(messageService.create).toHaveBeenCalledWith('error', 'The file must be a .txt or .csv');
  });
});
