import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Rule } from '../rule.model';
import { RulesService } from '../rules.service';

@Component({
  selector: 'app-rules-list',
  templateUrl: './rules-list.component.html',
  styleUrls: ['./rules-list.component.sass']
})
export class RulesListComponent implements OnInit {
  rules$: Observable<Rule[]>;
  loading: Observable<boolean>;

  constructor(private ruleService: RulesService) {
    this.rules$ = this.ruleService.entities$;
    this.loading = this.ruleService.loading$;
  }

  ngOnInit(): void {
  }

}
