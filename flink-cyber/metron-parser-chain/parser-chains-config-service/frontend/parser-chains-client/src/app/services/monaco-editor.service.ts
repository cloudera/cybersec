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
export type MonacoWindow = Window & typeof globalThis & { monaco: any };

@Injectable()
export class MonacoEditorService {

  private _registeredSchemas = {};

  /**
   * Creates a model with the given schema and store it. Returns with
   * the newly created model. If it is already registered with the given name (uri)
   * it just return with it from the "cache".
   */
  public registerSchema(uri: string, schema: any, options: {
    value?: string
  } = {}) {
    if (!this._registeredSchemas[uri]) {
      const { monaco } = window as MonacoWindow;
      monaco.languages.json.jsonDefaults.diagnosticsOptions.schemas.push(schema);
      const model = this._createModel(monaco, uri, options);
      this._registeredSchemas[uri] = model;
      return model;
    }
    return this._registeredSchemas[uri];
  }

  public getModel(uri: string) {
    return this._registeredSchemas[uri] || null;
  }

  /**
   * Internal function to generate a model using the appropriate helper
   * function in the monaco object.
   */
  private _createModel(monaco, uri, options: {
    value?: string
  } = {}) {
    const modelUri = monaco.Uri.parse(uri);
    return monaco.editor.createModel(options.value, 'json', modelUri);
  }
}
