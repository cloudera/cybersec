import { Component, OnInit } from '@angular/core';

type Properties = {
  [name: string]: string
};
export interface RestConfig {
  id: string;
  name: string;
  description: string;
  endpointTemplate: string;
  entityTemplate: string;
  method: string;
  properties: Properties;
  sources: string[];
  authorization: Properties;
  headers: Properties;
  timeoutMillis: number;
  capacity: number;
  cacheSize: number;
  prefix: string;
  successJsonPath: string;
  resultsJsonPath: string;
}

@Component({
  selector: 'app-rest-list',
  templateUrl: './rest-list.component.html',
  styleUrls: ['./rest-list.component.sass']
})
export class RestListComponent implements OnInit {
  expandSet = new Set<string>();

  data: RestConfig[] = [];

  onExpandChange(name: string, checked: boolean): void {
    if (checked) {
      this.expandSet.add(name);
    } else {
      this.expandSet.delete(name);
    }
  }
  constructor() { }

  ngOnInit(): void {
  }

}
