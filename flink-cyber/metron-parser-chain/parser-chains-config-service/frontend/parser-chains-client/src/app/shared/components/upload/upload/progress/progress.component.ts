import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-upload-progress',
  templateUrl: './progress.component.html',
  styleUrls: ['./progress.component.scss']
})
export class UploadProgressComponent {
  @Input() progress = 0;
  constructor() {}

}
