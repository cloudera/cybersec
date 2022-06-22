import { Component, Input } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { StoreModule } from '@ngrx/store';

import * as fromReducers from '../../chain-page.reducers';

import { ParserComposerComponent } from './parser-composer.component';

@Component({
  selector: 'app-parser',
  template: ''
})
class MockParserComponent {
  @Input() dirty = false;
  @Input() parser;
  @Input() configForm;
  @Input() isolatedParserView;
  @Input() parserType;
  @Input() failedParser;
  @Input() collapsed;
}

@Component({
  selector: 'app-router',
  template: ''
})
class MockRouterComponent extends MockParserComponent {}

describe('ParserComposerComponent', () => {
  let component: ParserComposerComponent;
  let fixture: ComponentFixture<ParserComposerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({
          'chain-page': fromReducers.reducer
        })
      ],
      declarations: [
        ParserComposerComponent,
        MockParserComponent,
        MockRouterComponent
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParserComposerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
