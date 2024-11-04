export interface OcsfSchemaModel {
  version: string,
  objects: { [key: string]: BaseOcsfSchemaModel },
  types: { [key: string]: OcsfType },
  classes: { [key: string]: BaseOcsfSchemaModel },
  base_event: BaseOcsfSchemaModel,
  dictionary_attributes: { [key: string]: { [key: string]: {} } };
}

export interface BaseOcsfSchemaModel {
  name: string,
  caption: string,
  description: string,
  profiles?: string[],
  attributes: { [key: string]: BaseOcsfTypeModel };
}

export interface BaseOcsfTypeModel {
  type: string,
  description: string,
  caption: string,
  requirement: string,
  type_name: string
  object_type?: string
}

export interface OcsfType {
  caption: string,
  description: string,
  max_len?: number
  type?: string,
  type_name?: string,
  range?: number[],
  observable?: number,
  regex?: string,
  values?: string[]
}
