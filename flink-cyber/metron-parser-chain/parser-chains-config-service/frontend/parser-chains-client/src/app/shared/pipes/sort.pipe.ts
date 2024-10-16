import {Pipe, PipeTransform} from "@angular/core";
import {sortBy, SortOrder} from '../utils';

@Pipe({name: "sortPipe"})
export class SortPipe implements PipeTransform {
  transform<T>(value: T[],column : keyof T, order:SortOrder = 'asc'): T[] {
    if (!value || value.length <= 1) {
      return value;
    }
    if (!column || column === '') {
      if (order === 'asc') {
        return value.sort();
      } else  {
        return value.sort().reverse();
      }
    }
    return sortBy(value, column, order);
  }

}
