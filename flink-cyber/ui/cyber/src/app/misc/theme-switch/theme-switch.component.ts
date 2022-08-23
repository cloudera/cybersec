import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-theme-switch',
  templateUrl: './theme-switch.component.html',
  styleUrls: ['./theme-switch.component.scss']
})
export class ThemeSwitchComponent implements OnInit {
  isDarkMode = false;

  ngOnInit() {
    this.isDarkMode = window.localStorage.getItem('dark-mode') === 'on';
    this.onChange(this.isDarkMode);
  }

  onChange(value) {
    document.body.classList[value ? 'add' : 'remove']('dark-mode');
    this.isDarkMode = value;
    window.localStorage.setItem('dark-mode', value ? 'on' : 'off');
  }
}
