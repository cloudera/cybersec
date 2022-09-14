import { Injectable } from '@angular/core';

@Injectable()
export class MonacoEditorService {

  private registeredSchemas = {};

  /**
   * Creates a model with the given schema and store it. Returns with
   * the newly created model. If it is already registered with the given name (uri)
   * it just return with it from the "cache".
   */
  public registerSchema(uri: string, schema: any, options: {
    value?: string
  } = {}) {
    if (!this.registeredSchemas[uri]) {
      monaco.languages.json.jsonDefaults.diagnosticsOptions.schemas.push(schema);
      const model = this.createModel(uri, options);
      this.registeredSchemas[uri] = model;
      return model;
    }
    return this.registeredSchemas[uri];
  }

  public getModel(uri: string) {
    return this.registeredSchemas[uri] || null;
  }

  /**
   * Internal function to generate a model using the appropriate helper
   * function in the monaco object.
   */
  private createModel(uri, options: {
    value?: string
  } = {}) {
    const modelUri = monaco.Uri.parse(uri);
    return monaco.editor.createModel(options.value, 'json', modelUri);
  }
}
