import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import {trigger, state, style, animate, transition} from '@angular/animations';
import {
  HttpClient, HttpRequest,
  HttpEventType, HttpErrorResponse
} from '@angular/common/http';
import {Subscription, of} from 'rxjs';
import {catchError, last, map, tap} from 'rxjs/operators';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss'],
  animations: [
    trigger('fadeInOut', [
      state('in', style({opacity: 100})),
      transition('* => void', [
        animate(300, style({opacity: 0}))
      ])
    ])
  ]
})
export class FileUploadComponent implements OnInit {
  /** Name used in form which will be sent in HTTP request. */
  @Input() param: string = 'file';
  /** Target URL for file uploading. */
  @Input() target: string;
  /** File extension that accepted, same as 'accept' of <input type="file" />.
   By the default, it's set to tar gz archive file */
  @Input() accept: string = "application/gzip, .gz";
  /** Allow you to add handler after its completion. Bubble up response text from remote. */
  @Output() preparedFiles = new EventEmitter<boolean>();

  @Output() inProgress = new EventEmitter<boolean>();

  files: FileUploadModel[] = [];

  constructor(
    private http: HttpClient,
  ) {
  }


  ngOnInit() {
    this.inProgress.emit(false);
    this.preparedFiles.emit(false);
  }

  onFileDropped(event: Event & { target: HTMLInputElement }) {
    this.prepareFilesList(event.target.files);
  }

  /**
   * handle file from browsing
   */
  fileBrowseHandler(event: Event) {
    const input = event.target as HTMLInputElement;
    this.prepareFilesList(input.files);
  }

  /**
   * Delete file from files list
   * @param index (File index)
   */
  private deleteFile(index: number) {
    this.files.splice(index, 1);
  }

  deleteAllFiles() {
    this.files = [];
    this.emitResults();
  }


  cancelFile(file: FileUploadModel, index: number) {
    file.sub.unsubscribe();
    this.deleteFile(index);
  }

  retryFile(file: FileUploadModel, index: number) {
    this.uploadFile(file, index);
    file.canRetry = false;
  }

  private uploadFile(file: FileUploadModel, index?: number) {
    const fd = new FormData();
    fd.append(this.param, file.data);

    const req = new HttpRequest('POST', this.target, fd, {
      reportProgress: true
    });

    file.sub = this.http.request(req).pipe(
      map(event => {
        switch (event.type) {
          case HttpEventType.UploadProgress:
            file.progress = Math.round(event.loaded * 100 / event.total);
            break;
          case HttpEventType.Response:
            return event;
        }
      }),
      tap(message => {
      }),
      last(),
      catchError((error: HttpErrorResponse) => {
        file.canRetry = true;
        return of(`${file.data.name} upload failed.`);
      })
    ).subscribe(
      (event: any) => {
        if (typeof (event) === 'object') {
          this.deleteFile(index);
          this.emitResults();
        }
      }
    );
  }

  uploadFiles() {
    this.files.forEach((file, index) => {
      this.uploadFile(file, index);
    });
  }

  /**
   * Convert Files list to normal array list
   * @param files (Files List)
   */
  prepareFilesList(files: FileList) {
    Array.from(files).forEach((file) => {
      this.files.push({data: file, progress: 0, canRetry: false, canCancel: true});
    });
    this.preparedFiles.emit(true);
  }


  formatBytes(bytes: number): string {
    if (bytes === 0) {
      return '0 Bytes';
    }
    const units = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return parseFloat((bytes / Math.pow(2, i)).toFixed(2)) + ' ' + units[i];
  }

  private emitResults() {
    if (this.files.length === 0) {
      this.inProgress.emit(false);
      this.preparedFiles.emit(true);
    }
  }
}

export class FileUploadModel {
  data: File;
  progress: number;
  canRetry: boolean;
  canCancel: boolean;
  sub?: Subscription;
}
