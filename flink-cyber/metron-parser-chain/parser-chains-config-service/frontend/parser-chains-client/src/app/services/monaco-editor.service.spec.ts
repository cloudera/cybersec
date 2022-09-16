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
