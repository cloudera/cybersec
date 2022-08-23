import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stack-trace',
  templateUrl: './stack-trace.component.html',
  styleUrls: ['./stack-trace.component.scss']
})
export class StackTraceComponent {

  @Input() stackTraceMsg = '';

}
