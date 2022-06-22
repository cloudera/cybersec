import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map } from 'rxjs/operators';

import { ParserModel } from '../chain-page/chain-page.models';


@Injectable({
  providedIn: 'root'
})
export class AddParserPageService {

  private readonly BASE_URL = '/api/v1/parserconfig/';

  constructor(
    private http: HttpClient
  ) {}

  public add(chainId: string, parser: ParserModel) {
    return this.http.post(this.BASE_URL + `chains/${chainId}/parsers`, parser);
  }

  public getParserTypes() {
    return this.http.get(this.BASE_URL + `parser-types`);
  }

  public getParsers(chainId: string) {
    return this.http.get(this.BASE_URL + `chains/${chainId}/parsers`)
      .pipe(
        map((parsers: ParserModel[]) => {
          return parsers;
        })
      );
  }
}
