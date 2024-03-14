import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {IndexingFormComponent} from './indexing-form.component';
import {NzFormModule} from "ng-zorro-antd/form";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MockComponent} from "ng-mocks";
import {AdvancedEditorComponent} from "../parser/advanced-editor/advanced-editor.component";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {provideMockStore} from "@ngrx/store/testing";
import {NzInputModule} from "ng-zorro-antd/input";
import {Store} from "@ngrx/store";
import {GetIndexMappingsAction} from "../../chain-page.actions";

describe('IndexingFormComponent', () => {
  let component: IndexingFormComponent;
  let fixture: ComponentFixture<IndexingFormComponent>;
  let store: Store;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzFormModule,
        NzInputModule,
        FormsModule,
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        provideMockStore({
          initialState: {
            'chain-page': {
              indexMappings: {
                path: "foo-path",
                result: {
                  value: "foo-value"
                }
              }
            }
          }
        })
      ],
      declarations: [IndexingFormComponent, MockComponent(AdvancedEditorComponent)]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IndexingFormComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);
    fixture.detectChanges();
  });

  it('should create', () => {
    spyOn(store, 'dispatch').and.callThrough();
    component.ngOnInit();
    expect(component).toBeTruthy();
    const action = new GetIndexMappingsAction({filePath: "foo-path"});
    expect(store.dispatch).toHaveBeenCalledWith(action);
  });

  it('should dispatch action and emit value on init', () => {
    spyOn(store, 'dispatch').and.callThrough();
    component.ngOnInit();
    expect(component).toBeTruthy();
    const action = new GetIndexMappingsAction({filePath: "foo-path"});
    expect(store.dispatch).toHaveBeenCalledWith(action);
  });
});
