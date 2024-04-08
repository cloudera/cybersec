import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {IndexingFormComponent} from './indexing-form.component';
import {NzFormModule} from "ng-zorro-antd/form";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MockComponent} from "ng-mocks";
import {AdvancedEditorComponent} from "../parser/advanced-editor/advanced-editor.component";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {NzInputModule} from "ng-zorro-antd/input";
import {NzMessageService} from "ng-zorro-antd/message";
import {ChainPageService} from "../../../services/chain-page.service";

describe('IndexingFormComponent', () => {
  let component: IndexingFormComponent;
  let fixture: ComponentFixture<IndexingFormComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzFormModule,
        NzInputModule,
        FormsModule,
        ReactiveFormsModule,
        NoopAnimationsModule,
      ],
      providers: [
        {provide: NzMessageService, useValue: jasmine.createSpyObj('NzMessageService', ['create'])},
        {provide: ChainPageService, useValue: jasmine.createSpyObj('ChainPageService', ['getIndexMappings'])},
      ],
      declarations: [IndexingFormComponent, MockComponent(AdvancedEditorComponent)]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IndexingFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
