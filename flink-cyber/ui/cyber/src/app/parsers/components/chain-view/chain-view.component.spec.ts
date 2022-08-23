import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, Input } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NzMessageServiceModule } from 'ng-zorro-antd/message';

import { ParserModel } from '../../parsers.models';

import { ChainViewComponent } from './chain-view.component';

@Component({
  selector: 'app-parser-composer',
  template: ''
})
class MockParserComposerComponent {
  @Input() parsers: ParserModel[];
  @Input() dirtyParsers;
  @Input() parserId;
  @Input() chainId;
  @Input() dirty;
  @Input() configForm;
  @Input() failedParser;
  @Input() collapsed;
}

describe('ChainViewComponent', () => {
  let component: ChainViewComponent;
  let fixture: ComponentFixture<ChainViewComponent>;
  const parsers: ParserModel[] = [
    {
      id: '123',
      name: 'Syslog',
      type: 'Grok',
      config: {},
    }
  ];

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        NzMessageServiceModule,
        NoopAnimationsModule,
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      declarations: [
        ChainViewComponent,
        MockParserComposerComponent,
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ChainViewComponent);
    component = fixture.componentInstance;
    component.parsers = parsers;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
