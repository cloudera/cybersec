import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'TMO Parser Chaining';
  isReverseArrow = false;
  width: string | number = 200;
  isCollapsed = false;
}
