/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
      const { monaco } = window as any;
      monaco.languages.json.jsonDefaults.diagnosticsOptions.schemas.push(schema);
      const model = this.createModel(monaco, uri, options);
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
  private createModel(monaco, uri, options: {
    value?: string
  } = {}) {
    const modelUri = monaco.Uri.parse(uri);
    return monaco.editor.createModel(options.value, 'json', modelUri);
  }
}
