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

  getIconType(): string {
    switch (this.iconName.toLowerCase()) {
      case 'stop': case 'stopped':
        return 'stop_circle_outline';
      case 'start' : case 'running':
        return 'play_circle_outline';
      case 'pause':
        return 'pause_circle_outline';
      case 'failed':
        return 'error';
      case 'online':
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
      case 'stop': case 'stopped':
        return 'red-icon';
      case 'start' : case 'running':
        return 'green-icon';
      case 'pause':
        return 'yellow-icon';
      case 'failed':
        return 'red-icon';
      case 'online':
        return 'green-icon';
      case 'offline':
        return 'red-icon';
      case 'restart' :
        return 'green-icon';
      default:
        return 'red-icon';
    }
  }

}
