import {Component, Input, OnInit} from "@angular/core";


@Component({
  selector: 'app-status-icon',
  templateUrl: './status-icon.component.html',
  styleUrls: ['./status-icon.component.scss']
})
export class StatusIconComponent implements OnInit {
  @Input() iconName: string;

  ngOnInit(): void {
  }

  isSpinner(): boolean {
    return this.iconName === "starting";
  }

  getIconType(): string {
    switch (this.iconName.toLowerCase()) {
      case 'stop':
      case 'stopped':
        return 'stop_circle_outline';
      case 'failed':
        return 'error';
      case 'start' :
        return 'play_circle_outline';
      case 'online':
      case 'running' :
        return 'check_circle_outline';
      case 'offline':
        return 'offline_bolt';
      case 'restart' :
        return 'replay';
      default:
        return 'error';
    }
  }

  getIconClass(): string {
    switch (this.iconName.toLowerCase()) {
      case 'stop':
      case 'failed':
        return 'red-icon';
      case 'stopped':
      case 'paused':
      case 'offline':
        return 'gray-icon';
      case 'running' :
      case 'start' :
      case 'online':
      case 'restart' :
        return 'green-icon';
      default:
        return 'red-icon';
    }
  }
}
