import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'contains'
})
export class ContainsPipe implements PipeTransform {

  transform(items: string[], term: string): string[] {
    if (items === null || items.length === 0 || term === null || term.length === 0) {
      return items;
    }
    return items.filter(item => item.toLowerCase().includes(term.toLowerCase()));
  }

}
