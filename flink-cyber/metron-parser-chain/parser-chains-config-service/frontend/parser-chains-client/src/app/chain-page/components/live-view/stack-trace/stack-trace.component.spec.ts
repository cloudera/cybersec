import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NzPopoverModule } from 'ng-zorro-antd/popover';

import { StackTraceComponent } from './stack-trace.component';

describe('StackTraceComponent', () => {
  let component: StackTraceComponent;
  let fixture: ComponentFixture<StackTraceComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        NzPopoverModule,
      ],
      declarations: [ StackTraceComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StackTraceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
