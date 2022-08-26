import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Store, StoreModule } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { of } from 'rxjs';

import * as fromChainPageReducers from '../chain-page/chain-page.reducers';

import { ChainAddParserPageComponent } from './chain-add-parser-page.component';
import { AddParserPageState, reducer } from './chain-add-parser-page.reducers';

const fakeActivatedRoute = {
  params: of({
    id: '456'
  })
};

describe('ChainAddParserPageComponent', () => {
  let component: ChainAddParserPageComponent;
  let fixture: ComponentFixture<ChainAddParserPageComponent>;
  let store: Store<AddParserPageState>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ChainAddParserPageComponent ],
      imports: [
        NzModalModule,
        FormsModule,
        ReactiveFormsModule,
        NoopAnimationsModule,
        StoreModule.forRoot({
          'chain-add-parser-page': reducer,
          'parsers': fromChainPageReducers.reducer
        }),
        RouterTestingModule
      ],
      providers: [
        provideMockStore({ initialState: {
          'chain-add-parser-page': {},
          'parsers': {
            chains: {
              456: {}
            }
          }
        } }),
        { provide: ActivatedRoute, useFactory: () => fakeActivatedRoute }
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ChainAddParserPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    store = TestBed.get(Store);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the main chain', () => {
    const spy = spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(spy).toHaveBeenCalled();
  });
});
