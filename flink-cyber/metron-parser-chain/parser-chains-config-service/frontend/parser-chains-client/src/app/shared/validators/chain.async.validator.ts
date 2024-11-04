import {AbstractControl, AsyncValidator, ValidationErrors} from '@angular/forms';
import {Observable, of} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';
import {unique} from 'src/app/shared/utils';
import {ChainListPageService} from 'src/app/services/chain-list-page.service';
import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ChainAsyncValidator implements AsyncValidator {
  constructor(private _service: ChainListPageService
  ) {
  }

  validate(control: AbstractControl):
    Promise<ValidationErrors | null> | Observable<ValidationErrors | null> {
    const uniqueFunc = unique(control.value, 'name');
    return this._service.getChains().pipe(
      switchMap(chains => uniqueFunc(chains) ? of({uniqueValue: true}) : of(null)),
      catchError(() => of({httpError: true}))
    );
  }
}
