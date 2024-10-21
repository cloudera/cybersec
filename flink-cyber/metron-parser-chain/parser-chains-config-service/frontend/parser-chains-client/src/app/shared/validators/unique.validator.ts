import {Observable, of} from 'rxjs';
import {AbstractControl, ValidationErrors} from '@angular/forms';
import {catchError, map} from 'rxjs/operators';


export class UniqueValidator {
  static unique<T>(items: Observable<T[]>, control: AbstractControl, column: keyof T = null): Observable<ValidationErrors | null> {
    const uniqVal = control.value.toLowerCase();
    return items.pipe(
      map(el => {
        if (column) {
          return el.some(item => String(item[column]).toLowerCase() === uniqVal) ? {uniqueValue: true} : null;
        }
        return el.some(item => String(item).toLowerCase() === uniqVal) ? {uniqueValue: true} : null
      }),
      catchError(_ => {
        return of({error: true});
      }),
    );

    // return values.pipe(
    //   map(items => {
    //     if (column) {
    //       const b = items.some(el => el[column] === uniqVal);
    //       return b ? {uniqueValue: true} : null;
    //     }
    //     return items.some(uniqVal) ? {uniqueValue: true}: null;
    //   }),
    //   first()
    // );
  }
}
