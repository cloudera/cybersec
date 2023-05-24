import {Component, Input, OnInit} from "@angular/core";
import {b} from "msw/lib/glossary-de6278a9";

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
    return this.iconName === "running";
  }

  getIconType(): string {
    switch (this.iconName.toLowerCase()) {
      case 'stop':
      case 'stopped':
        return 'stop_circle_outline';
      case 'paused':
        return 'pause_circle_outline';
      case 'failed':
        return 'error';
      case 'start' :
        return 'play_circle_outline';
      case 'online':
      case 'started' :
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
      case 'started' :
      case 'start' :
      case 'online':
      case 'restart' :
        return 'green-icon';
      default:
        return 'red-icon';
    }
  }

}
