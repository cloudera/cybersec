import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { IconDefinition } from '@ant-design/icons-angular';
import { DeleteFill, PlusOutline, RightSquareFill } from '@ant-design/icons-angular/icons';
import { Store } from '@ngrx/store';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NZ_ICONS } from  'ng-zorro-antd/icon'
import { of } from 'rxjs';

import { ChainListPageService } from '../services/chain-list-page.service';
import * as fromActions from './chain-list-page.actions';
import { ChainListPageComponent } from './chain-list-page.component';
import { ChainModel } from './chain.model';

const icons: IconDefinition[] = [PlusOutline, DeleteFill, RightSquareFill];

class FakeChainListPageService {
  getChains() {
    return of([{
      id: 'id1',
      name: 'Chain 1'
    }, {
      id: 'id2',
      name: 'Chain 2'
    }, {
      id: 'id3',
      name: 'Chain 3'
    }]);
  }

  deleteChain() {
    return of([]);
  }

  createChain() {
    return of({});
  }
}

describe('ChainListPageComponent', () => {
  let component: ChainListPageComponent;
  let fixture: ComponentFixture<ChainListPageComponent>;
  let service: ChainListPageService;

  let store: MockStore<{
    'chain-list-page': {
      loading: boolean;
      error: string;
      items: ChainModel[];
    }
  }>;

  const initialState = {
    'chain-list-page': {
      loading: false,
      error: '',
      items: [
        { id: 'id1', name: 'Chain 1' },
        { id: 'id2', name: 'Chain 2' },
        { id: 'id3', name: 'Chain 3' }
      ]
    }
  };

  function clickOkOnPopConfirm() {
    (document.querySelector(
      '.ant-popover .ant-btn-primary'
    ) as HTMLElement).click();
  }

  function clickDeleteBtnOnIndex(index: number) {
    fixture.debugElement
      .queryAll(By.css('.chain-delete-btn'))
      [index].nativeElement.click();
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        FormsModule,
        ReactiveFormsModule,
        RouterTestingModule,
        NoopAnimationsModule,
      ],
      declarations: [ChainListPageComponent],
      providers: [
        provideMockStore({ initialState }),
        {
          provide: ChainListPageService,
          useClass: FakeChainListPageService
        },
        { provide: NZ_ICONS, useValue: icons }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.get(Store);
    service = TestBed.get(ChainListPageService);
    fixture = TestBed.createComponent(ChainListPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show up confimation when user click the delete button', () => {

    fixture.detectChanges();

    const indexOfSecondDeleteBtn = 1;
    clickDeleteBtnOnIndex(indexOfSecondDeleteBtn);
    fixture.detectChanges();
    const popover = document.querySelector('.ant-popover');
    expect(popover).toBeTruthy();
  });

  it('should dispatch an action to delete the chain', () => {
    spyOn(store, 'dispatch').and.callThrough();
    fixture.detectChanges();
    const index = 0;
    clickDeleteBtnOnIndex(index);
    fixture.detectChanges();

    clickOkOnPopConfirm();
    fixture.detectChanges();
    const action = new fromActions.DeleteChainAction('id1', 'Chain 1');

    fixture.detectChanges();
    expect(store.dispatch).toHaveBeenCalledWith(action);
  });

  it('should change the sort order to descending', () => {
    component.sortDescription$.next({key: 'name', value: 'descend'});
    fixture.detectChanges();
    component.chainDataSorted$.subscribe(
      data => expect(data[0].name).toBe('Chain 3')
    );
  });

  it('should change the sort order to ascending', () => {
    component.sortDescription$.next({key: 'name', value: 'ascend'});
    fixture.detectChanges();
    component.chainDataSorted$.subscribe(
      data => expect(data[0].name).toBe('Chain 1')
    );
  });

  it('should call the pushValue function', () => {
    const addBtn = fixture.nativeElement.querySelector('[data-qe-id="add-chain-btn"]');
    addBtn.click();
    fixture.detectChanges();

    const chainNameField: HTMLInputElement = fixture.debugElement.queryAll(By.css('[data-qe-id="chain-name"]'))[0].nativeElement;
    expect(chainNameField).toBeDefined();

    const pushMethodSpy = spyOn(component, 'pushChain');
    chainNameField.value = 'New Chain';
    chainNameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const createBtn = fixture.debugElement.queryAll(By.css('.ant-modal .ant-btn-primary'))[0].nativeElement;
    createBtn.click();
    fixture.detectChanges();
    expect(pushMethodSpy).toHaveBeenCalledTimes(1);
  });

  it('should call the CreateChainAction', () => {
    spyOn(store, 'dispatch').and.callThrough();

    const addBtn = fixture.nativeElement.querySelector('[data-qe-id="add-chain-btn"]');
    addBtn.click();
    fixture.detectChanges();

    const chainNameField: HTMLInputElement = fixture.debugElement.queryAll(By.css('[data-qe-id="chain-name"]'))[0].nativeElement;
    expect(chainNameField).toBeDefined();

    chainNameField.value = 'New Chain';
    chainNameField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const createBtn = fixture.debugElement.queryAll(By.css('.ant-modal .ant-btn-primary'))[0].nativeElement;
    createBtn.click();
    fixture.detectChanges();
    const action = new fromActions.CreateChainAction({name: 'New Chain'});

    expect(store.dispatch).toHaveBeenCalledWith(action);
  });
});
