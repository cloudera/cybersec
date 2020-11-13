export enum RuleType {
  JS, PYTHON, STELLAR
}

export interface Rule {
  id: string;
  order: number;
  name: string;
  tsStart: Date;
  tsEnd: Date;
  type: RuleType;
  script: string;
  enabled: boolean;
  reason: string;
}
