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

  it('should handle complex object', () => {
    spyOn(component.fieldSetUpdated, 'emit');
    const event = {
      value: {
        "squid": {
          "table_name": "hive_table",
          "ignore_fields": ["code"],
          "column_mapping": [{"name": "full_hostname"}, {"name": "action"}]
        },
        "test": {
          "table_name": "another_hive_table",
          "ignore_fields": ["foo"],
          "column_mapping": []
        }
      }
    };

    component.onAdvancedEditorChanged(event);

    expect(component.fieldSetUpdated.emit).toHaveBeenCalledWith({
      squid: {
        code: true,
        full_hostname: false,
        action: false
      },
      test: {
        foo: true
      }
    });
  });

  it('should handle object with no fields', () => {
    spyOn(component.fieldSetUpdated, 'emit');
    const event = {
      value: {
        "squid": {
          "table_name": "hive_table",
          "ignore_fields": [],
          "column_mapping": []
        },
        "test": {
          "table_name": "another_hive_table",
          "ignore_fields": [],
          "column_mapping": []
        }
      }
    };
    component.onAdvancedEditorChanged(event);

    expect(component.fieldSetUpdated.emit).toHaveBeenCalledWith({
    });
  });

  it('should handle object with empty', () => {
    spyOn(component.fieldSetUpdated, 'emit');
    const event = {
      value: {
      }
    };

    component.onAdvancedEditorChanged(event);

    expect(component.fieldSetUpdated.emit).not.toHaveBeenCalled();
  });

  it('should handle object with null', () => {
    spyOn(component.fieldSetUpdated, 'emit');
    const event = {
      value: null
    };

    component.onAdvancedEditorChanged(event);

    expect(component.fieldSetUpdated.emit).not.toHaveBeenCalled();
  });
});
