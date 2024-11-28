import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {formatBytes} from 'src/app/shared/utils';

@Component({
  selector: 'app-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent {
  /** File extension that accepted, same as 'accept' of <input type="file" />.
   By the default, it's set to tar gz archive file */
  private _multiple = false;

  protected readonly formatBytes = formatBytes;

  @Input() accept = 'application/gzip, .gz';
  @Input() progress = false;
  @Input() progressFunction: (name: string | undefined) => number;
  // /** Allow you to add handler after its completion. Bubble up response text from remote. */
  @Output() filesEmitter = new EventEmitter<File[]>();
  @ViewChild('input_upload') input: ElementRef;
  files: File[] = [];

  @Input()
  set multiple(value: boolean | string) {
    this._multiple = value != null && `${value}` !== 'false';
  }

  get multiple(): boolean {
    return this._multiple;
  }

  filesValidate(fileList: FileList) {
    Array.from(fileList).forEach((file) => {
      this.files.push(file);
    });
  }

  cancelFile(name: string) {
    this.files = this.files.filter(file => file.name !== name);
    this.input.nativeElement.value = '';
  }

  buttonClick() {
    this.filesEmitter.emit(this.files);
  }

  getProgress(name: string | undefined) {
    return this.progressFunction(name);
  }

}
