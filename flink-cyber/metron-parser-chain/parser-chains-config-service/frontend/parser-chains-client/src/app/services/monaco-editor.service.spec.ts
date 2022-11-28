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

import { TestBed } from '@angular/core/testing';

import { MonacoEditorService } from './monaco-editor.service';

describe('MonacoEditorService', () => {
  let service: MonacoEditorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ MonacoEditorService ]
    });
    service = TestBed.get(MonacoEditorService);
  });

  it('should register a model once with the given schema', () => {
    // mocking the monaco object in the global namespace (window)
    const modelToCreate = {};
    (window as any).monaco = {
      languages: { json: { jsonDefaults: { diagnosticsOptions: { schemas: [] } } } },
      Uri: { parse: (val) => val },
      editor: { createModel: () => modelToCreate }
    };

    const schema = {};
    let model = service.registerSchema('foo', schema);
    expect(model).toBe(modelToCreate);

    // should return with the same model if already registered
    model = service.registerSchema('foo', schema);
    expect(model).toBe(modelToCreate);

    // should return with the registered schema
    model = service.getModel('foo');
    expect(model).toBe(modelToCreate);

    // should return with null if schema is not registered
    model = service.getModel('bar');
    expect(model).toBeNull();
  });
});
