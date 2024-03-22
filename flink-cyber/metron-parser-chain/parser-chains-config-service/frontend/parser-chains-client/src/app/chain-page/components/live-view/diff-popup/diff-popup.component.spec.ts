import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {DiffPopupComponent} from './diff-popup.component';
import {MockStore, provideMockStore} from "@ngrx/store/testing";
import {getDiffModalVisible, getNewDiffValue, getPreviousDiffValue} from "./diff-popup.selectors";
import {Store} from "@ngrx/store";
import {DiffPopupState} from "./diff-popup.reducers";
import {CommonModule} from "@angular/common";
import {NzModalModule} from "ng-zorro-antd/modal";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {TextDiffViewComponent} from "../text-diff-view/text-diff-view.component";
import {MockComponent} from "ng-mocks";
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";

describe('DiffPopupComponent', () => {
  let component: DiffPopupComponent;
  let fixture: ComponentFixture<DiffPopupComponent>;
  let store: MockStore<DiffPopupState>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [DiffPopupComponent, MockComponent(TextDiffViewComponent)],
      imports: [
        CommonModule,
        NoopAnimationsModule,
        NzModalModule,
      ],
      providers: [
        provideMockStore({
          initialState: {diffModalVisible: false, previousDiffValue: '', newDiffValue: ''},
          selectors: [
            {selector: getDiffModalVisible, value: false},
            {selector: getPreviousDiffValue, value: ''},
            {selector: getNewDiffValue, value: ''}
          ]
        })
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DiffPopupComponent);
    component = fixture.componentInstance;
    store = TestBed.inject(Store) as MockStore<DiffPopupState>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch HideDiffModalAction on handleOkModal', () => {
    spyOn(store, 'dispatch');
    component.handleOkModal();
    expect(store.dispatch).toHaveBeenCalled();
  });

  it('should dispatch HideDiffModalAction on handleCancelModal', () => {
    spyOn(store, 'dispatch');
    component.handleCancelModal();
    expect(store.dispatch).toHaveBeenCalled();
  });

  it('should notRender window by default and show it on set for modalVisible', () => {
    let visible: boolean;
    component.diffModalVisible$.subscribe((value) => visible = value);

    expect(visible).toBeFalsy();

    store.overrideSelector(getDiffModalVisible, true);
    store.overrideSelector(getPreviousDiffValue, 'prev');
    store.overrideSelector(getNewDiffValue, 'new');
    store.refreshState();
    fixture.detectChanges();

    expect(visible).toBeTruthy();
    const container = document.querySelector('app-text-diff-view');
    expect(container).not.toBeNull();
  });
});
