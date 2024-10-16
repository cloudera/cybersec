import {Pipe, PipeTransform} from '@angular/core';
import {Observable, of} from 'rxjs';
import {catchError, map, startWith} from 'rxjs/operators';

export interface ObsWrapper<T> {
  loading?: boolean;
  value?: T;
  error?: string;
}

const defaultError = 'Something went wrong';

@Pipe({
  name: 'withLoading',
})
export class WithLoadingPipe implements PipeTransform {
  transform<T = any>(val: Observable<T>): Observable<ObsWrapper<T>> {
    return val.pipe(
      map((value: any) => {
        return {
          loading: value.type === 'start',
          error: value.type === 'error' ? defaultError : '',
          value: value.type ? value.value : value,
        };
      }),
      startWith({ loading: true }),
      catchError(error => of({ loading: false, error: typeof error === 'string' ? error : defaultError }))
    );
  }
}
