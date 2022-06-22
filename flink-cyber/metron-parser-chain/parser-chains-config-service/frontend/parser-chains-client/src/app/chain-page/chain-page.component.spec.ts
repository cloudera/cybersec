import { Component, Input } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { IconDefinition } from '@ant-design/icons-angular';
import { EditFill, PlusOutline } from '@ant-design/icons-angular/icons';
import { StoreModule } from '@ngrx/store';
import { Store } from '@ngrx/store';
import { NgZorroAntdModule, NZ_ICONS } from 'ng-zorro-antd';
import { Observable, of } from 'rxjs';

import * as fromActions from './chain-page.actions';
import { ChainPageComponent } from './chain-page.component';
import { ParserModel } from './chain-page.models';
import * as fromReducers from './chain-page.reducers';
import * as fromLiveViewReducers from './components/live-view/live-view.reducers';

const icons: IconDefinition[] = [EditFill, PlusOutline];
@Component({
  selector: 'app-chain-view',
  template: ''
})
class MockChainViewComponent {
  @Input() parsers: ParserModel[];
  @Input() dirtyParsers;
  @Input() chainId;
  @Input() failedParser;
}

@Component({
  selector: 'app-live-view',
  template: ''
})
class MockLiveViewComponent {
  @Input() chainConfig$: Observable<{}>;
}

const fakeActivatedRoute = {
  params: of({})
};

describe('ChainPageComponent', () => {
  let component: ChainPageComponent;
  let fixture: ComponentFixture<ChainPageComponent>;
  let store: Store<ChainPageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        NgZorroAntdModule,
        StoreModule.forRoot({
          'chain-page': fromReducers.reducer,
          'live-view': fromLiveViewReducers.reducer
        }),
        NoopAnimationsModule,
        ReactiveFormsModule,
      ],
      declarations: [ChainPageComponent, MockChainViewComponent, MockLiveViewComponent],
      providers: [
        { provide: ActivatedRoute, useFactory: () => fakeActivatedRoute },
        { provide: Router, useValue: { events: of({}) } },
        { provide: NZ_ICONS, useValue: icons }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.get(Store);
    fixture = TestBed.createComponent(ChainPageComponent);
    component = fixture.componentInstance;
    component.chain = {
      id: '1',
      name: 'chain',
      parsers: []
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the popconfirm textbox for updating the chain name', () => {
    fixture.detectChanges();
    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    expect(editBtn).toBeTruthy();
    fixture.detectChanges();

    const nameField = document.querySelector('[data-qe-id="chain-name-field"]');
    expect(nameField).toBeTruthy();
  });

  it('should disable the chain name set btn if input length < 3', () => {
    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'aa';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    expect(submitBtn.disabled).toBe(true);
  });

  it('should enable the chain name set btn if input length > 3', () => {
    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'aaa';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    expect(submitBtn.disabled).toBe(false);
  });

  it('should call the onChainNameEditDone()', () => {
    spyOn(component, 'onChainNameEditDone');

    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'hello';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    submitBtn.click();
    fixture.detectChanges();

    expect(component.onChainNameEditDone).toHaveBeenCalled();
  });

  it('onChainNameEditDone() will call the UpdateChain and SetDirty Actions', () => {
    spyOn(store, 'dispatch');

    const editBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-qe-id="chain-name-edit-btn"]');
    editBtn.click();
    fixture.detectChanges();

    const nameField: HTMLInputElement = document.querySelector('[data-qe-id="chain-name-field"]');
    nameField.value = 'new_name';
    nameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = document.querySelector('[data-qe-id="edit-chain-name-submit-btn"]');
    submitBtn.click();
    fixture.detectChanges();

    const actionUpdate = new fromActions.UpdateChainAction({chain: {id: '1', name: 'new_name'}});

    expect(store.dispatch).toHaveBeenCalledWith(actionUpdate);
  });

  it('should pass the id of a failed parser if investigated', () => {
    component.parserToBeInvestigated = ['1111'];
    fixture.detectChanges();
    expect(component.parsers).toBe(component.parserToBeInvestigated);

    component.parserToBeInvestigated = [];
    component.chain.parsers = [{
      id: '123',
      type: 'test type',
      name: 'test name'
    }];
    fixture.detectChanges();
    expect(component.parsers).toBe(component.chain.parsers);
  });

});
