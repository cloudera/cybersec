import { Component, EventEmitter, Input, Output } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DeleteFill } from '@ant-design/icons-angular/icons';
import { NzModalModule } from 'ng-zorro-antd/modal';
import {  NZ_ICONS } from 'ng-zorro-antd/icon';


import { ConfigChangedEvent } from './advanced-editor/advanced-editor.component';
import { ParserComponent } from './parser.component';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

@Component({
  selector: 'app-custom-form',
  template: '',
})
export class MockCustomFormComponent {
  @Input() config = [];
}

@Component({
  selector: 'app-advanced-editor',
  template: '',
})
export class MockAdvancedEditorComponent {
  @Input() config = [];
  @Output() configChanged = new EventEmitter<ConfigChangedEvent>();
}

describe('ParserComponent', () => {
  let component: ParserComponent;
  let fixture: ComponentFixture<ParserComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        NzModalModule,
        NoopAnimationsModule,
        MonacoEditorModule,
      ],
      declarations: [
        ParserComponent,
        MockCustomFormComponent,
        MockAdvancedEditorComponent,
      ],
      providers : [
        { provide: NZ_ICONS, useValue: [DeleteFill]}
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParserComponent);
    component = fixture.componentInstance;
    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set a default value if no value in parser', () => {
    const fields = component.updateFormValues('config', [{
      id: 'foo',
      name: 'foo',
      type: 'text',
      path: 'config.a.b.c.d',
      defaultValue: 'default'
    }]);
    expect(fields[0].value).toBe('default');
  });

  it('should set a empty string if no either value in parser or defaultValue in field', () => {
    const fields = component.updateFormValues('config', [{
      id: 'foo',
      name: 'foo',
      type: 'text',
      path: 'config.a.b.c.d',
    }]);
    expect(fields[0].value).toBe('');
  });

  it('should get the value from the parser', () => {
    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro',
      config: { a: { b: { c: { d: { foo: 'ABCD' } } } } }
    };
    let fields = component.updateFormValues('config', [{
      id: 'foo',
      name: 'foo',
      type: 'text',
      path: 'config.a.b.c.d',
    }]);
    expect(fields[0].value).toBe('ABCD');

    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro',
      config: { foo: 'ABCD' }
    };
    fields = component.updateFormValues('config', [{
      id: 'foo',
      name: 'foo',
      type: 'text',
      path: 'config',
    }]);
    expect(fields[0].value).toEqual('ABCD');

    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro',
      config: {
        outputFields: {
          fieldName: 'field name',
          fieldIndex: 'field index'
        }
      }
    };
    fields = component.updateFormValues('config', [{
      id: 'fieldName',
      name: 'fieldName',
      type: 'text',
      path: 'config.outputFields',
    }, {
      id: 'fieldIndex',
      name: 'fieldIndex',
      type: 'text',
      path: 'config.outputFields',
    }]);
    expect(fields[0].value).toEqual('field name');
    expect(fields[1].value).toEqual('field index');
  });

  it('should update values in the parser properly', () => {
    component.configForm = [{
      id: 'fieldName',
      name: 'fieldName',
      type: 'text',
      path: 'config.outputFields',
      value: 'field name UPDATED'
    }, {
      id: 'fieldIndex',
      name: 'fieldIndex',
      type: 'text',
      path: 'config.outputFields',
      value: 'field index UPDATED'
    }];
    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro',
      config: {
        outputFields: {
          fieldName: 'field name',
          fieldIndex: 'field index'
        }
      }
    };
    component.ngOnInit();
    component.parserChange.subscribe(() => {
      expect(component.parser.config.outputFields.fieldName).toBe('field name UPDATED');
      expect(component.parser.config.outputFields.fieldIndex).toBe('field index');
    }).unsubscribe();
    component.onCustomFormChange(component.parser, component.configForm[0]);

    component.parserChange.subscribe(() => {
      expect(component.parser.config.outputFields.fieldName).toBe('field name UPDATED');
      expect(component.parser.config.outputFields.fieldIndex).toBe('field index UPDATED');
    }).unsubscribe();
    component.onCustomFormChange(component.parser, component.configForm[0]);
    component.onCustomFormChange(component.parser, component.configForm[1]);
  });

  it('should setup the form fields properly', () => {
    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro',
      config: {
        outputFields: {
          fieldName: 'field name INIT',
          fieldIndex: 'field index INIT'
        }
      }
    };
    const fields = component.setFormFieldValues([{
      name: 'whatever',
      type: 'text',
      path: '',
    }, {
      name: 'input',
      type: 'text',
      path: 'config',
    }, {
      name: 'fieldName',
      type: 'text',
      path: 'config.outputFields',
    }, {
      name: 'fieldIndex',
      type: 'text',
      path: 'config.outputFields',
    }]);
    expect(fields[0].id).toBe('123-whatever');
    expect(fields[1].id).toBe('123-config.input');
    expect(fields[2].id).toBe('123-config.outputFields.fieldName');
    expect(fields[3].id).toBe('123-config.outputFields.fieldIndex');

    expect(fields[0].value).toBe('');
    expect(fields[1].value).toBe('');
    expect(fields[2].value).toBe('field name INIT');
    expect(fields[3].value).toBe('field index INIT');
  });

  it('should dispatch action if config changed by the advanced editor', () => {
    const mockListener = jasmine.createSpy('mockListener');
    const value = { someField: 'some value' };

    component.parserChange.subscribe(mockListener);

    component.onAdvancedEditorChanged({ value });

    expect(mockListener).toHaveBeenCalledWith({
      id: '123',
      config: value,
    });
  });

  it('should style a parser as failed', () => {
    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro',
      config: {
        outputFields: {
          fieldName: 'field name INIT',
          fieldIndex: 'field index INIT'
        }
      }
    };
    component.failedParser = '123';
    fixture.detectChanges();
    const card = fixture.debugElement.query(By.css('[data-qe-id="chain-item"]'));
    expect(component.parsingFailed).toBe(true);
    expect(card.classes.failed).toBeTruthy();

    component.failedParser = '';
    fixture.detectChanges();
    expect(component.parsingFailed).toBe(false);
    expect(card.classes.failed).toBeFalsy();
  });

  it('should update multi value parsers properly', (done) => {
    const cases = [
      {
        input: {
          parser: {
            id: '123',
            name: 'some parser',
            type: 'foo',
            config: {
              foo: [{
                bar: 'value 1'
              }]
            }
          },
          formConfig: {
            name: 'bar',
            type: 'text',
            multiple: true,
            path: 'config.foo',
            value: [{
              foo: 'value 2'
            }, {
              bar: 'value 3'
            }]
          }
        },
        expected: {
          id: '123',
          name: 'some parser',
          type: 'foo',
          config: {
            foo: [{
              bar: 'value 1',
              foo: 'value 2'
            }, {
              bar: 'value 3'
            }]
          }
        }
      },
      {
        input: {
          parser: {
            id: '123',
            name: 'some parser',
            type: 'foo',
            config: {}
          },
          formConfig: {
            name: 'bar',
            type: 'text',
            multiple: true,
            path: 'config.foo',
            value: [{
              foo: 'value 2'
            }, {
              bar: 'value 3'
            }]
          }
        },
        expected: {
          id: '123',
          name: 'some parser',
          type: 'foo',
          config: {
            foo: [{
              foo: 'value 2'
            }, {
              bar: 'value 3'
            }]
          }
        }
      },
      {
        input: {
          parser: {
            id: '123',
            name: 'some parser',
            type: 'foo',
            config: {
              foo: [{
                bar: 'a'
              }, {
                bar: 'b'
              }, {
                bar: 'c'
              }, {
                bar: 'd'
              }, {
                bar: 'e'
              }]
            }
          },
          formConfig: {
            name: 'lorem',
            type: 'text',
            multiple: true,
            path: 'config.foo',
            value: [{
              bak: '1'
            }, {
              bak: '1'
            }, {
              bak: 'a+b'
            }, {
              bak: 'b+c'
            }, {
              bak: 'c+d'
            }, {
              bak: 'd+e'
            }]
          }
        },
        expected: {
          id: '123',
          name: 'some parser',
          type: 'foo',
          config: {
            foo: [{
              bar: 'a',
              bak: '1'
            }, {
              bar: 'b',
              bak: '1'
            }, {
              bar: 'c',
              bak: 'a+b'
            }, {
              bar: 'd',
              bak: 'b+c'
            }, {
              bar: 'e',
              bak: 'c+d'
            }, {
              bak: 'd+e'
            }]
          }
        }
      },
      {
        input: {
          parser: {
            id: '123',
            name: 'some parser',
            type: 'foo',
            config: {
              foo: [{
                bar: 'a'
              }, {
                bar: 'b'
              }]
            }
          },
          formConfig: {
            name: 'lorem',
            type: 'text',
            multiple: true,
            path: 'config.foo',
            value: [{
              bak: '1'
            }, {
              bak: '1'
            }, {
              bak: 'a+b'
            }, {
              bak: 'b+c'
            }]
          }
        },
        expected: {
          id: '123',
          name: 'some parser',
          type: 'foo',
          config: {
            foo: [{
              bar: 'a',
              bak: '1'
            }, {
              bar: 'b',
              bak: '1'
            }, {
              bak: 'a+b'
            }, {
              bak: 'b+c'
            }]
          }
        }
      },
      {
        input: {
          parser: {
            id: '123',
            name: 'some parser',
            type: 'foo',
            lorem: [{
              bar: 'a'
            }, {
              bar: 'b'
            }]
          },
          formConfig: {
            name: 'lorem',
            type: 'text',
            multiple: true,
            value: [{
              bak: '1'
            }, {
              bak: '1'
            }, {
              bak: 'a+b'
            }, {
              bak: 'b+c'
            }]
          }
        },
        expected: {
          id: '123',
          name: 'some parser',
          type: 'foo',
          lorem: [{
            bar: 'a',
            bak: '1'
          }, {
            bar: 'b',
            bak: '1'
          }, {
            bak: 'a+b'
          }, {
            bak: 'b+c'
          }]
        }
      },
      {
        input: {
          parser: {
            id: '123',
            name: 'some parser',
            type: 'foo',
          },
          formConfig: {
            name: 'lorem',
            type: 'text',
            multiple: true,
            value: [{
              bak: '1'
            }, {
              bak: '1'
            }, {
              bak: 'a+b'
            }, {
              bak: 'b+c'
            }]
          }
        },
        expected: {
          id: '123',
          name: 'some parser',
          type: 'foo',
          lorem: [{
            bak: '1'
          }, {
            bak: '1'
          }, {
            bak: 'a+b'
          }, {
            bak: 'b+c'
          }]
        }
      },
    ];

    cases.forEach((testCase, i) => {
      const localFixture = TestBed.createComponent(ParserComponent);
      const localComponent = localFixture.componentInstance;
      localComponent.parserChange.subscribe((partialParser) => {
        expect(partialParser).toEqual(testCase.expected);
        if (i === cases.length - 1) {
          done();
        }
      });
      localComponent.onCustomFormChange(testCase.input.parser, testCase.input.formConfig);
    });
  });

  describe('parser name editing', () => {
    it('should switch to edit mode if user clicks on parser name', () => {
      let nameEl = fixture.debugElement.query(By.css('[data-qe-id="parser-name"]')).nativeElement;
      nameEl.click();
      fixture.detectChanges();

      nameEl = fixture.debugElement.query(By.css('[data-qe-id="parser-name"]'));
      expect(nameEl).toBeFalsy();

      const inputField = fixture.debugElement.query(By.css('[data-qe-id="parser-name-input"]')).nativeElement;
      expect(inputField).toBeTruthy();
    });

    it('should switch to normal mode if edit input losing focus', () => {
      let nameEl = fixture.debugElement.query(By.css('[data-qe-id="parser-name"]')).nativeElement;
      nameEl.click();
      fixture.detectChanges();

      let inputField = fixture.debugElement.query(By.css('[data-qe-id="parser-name-input"]')).nativeElement;
      expect(inputField).toBeTruthy();

      inputField.dispatchEvent(new Event('blur'));
      fixture.detectChanges();

      inputField = fixture.debugElement.query(By.css('[data-qe-id="parser-name-input"]'));
      expect(inputField).toBeFalsy();

      nameEl = fixture.debugElement.query(By.css('[data-qe-id="parser-name"]'));
      expect(nameEl).toBeTruthy();
    });

    it('should switch to normal mode on enter', () => {
      let nameEl = fixture.debugElement.query(By.css('[data-qe-id="parser-name"]')).nativeElement;
      nameEl.click();
      fixture.detectChanges();

      let inputField = fixture.debugElement.query(By.css('[data-qe-id="parser-name-input"]')).nativeElement;
      expect(inputField).toBeTruthy();

      inputField.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
      fixture.detectChanges();

      inputField = fixture.debugElement.query(By.css('[data-qe-id="parser-name-input"]'));
      expect(inputField).toBeFalsy();

      nameEl = fixture.debugElement.query(By.css('[data-qe-id="parser-name"]'));
      expect(nameEl).toBeTruthy();
    });

    it('should propagate name change', () => {
      const mockListener = jasmine.createSpy('mockListener');
      component.parserChange.subscribe(mockListener);

      fixture.debugElement.query(By.css('[data-qe-id="parser-name"]')).nativeElement.click();
      fixture.detectChanges();

      const inputField = fixture.debugElement.query(By.css('[data-qe-id="parser-name-input"]')).nativeElement;
      inputField.value = 'edited parser name';
      inputField.dispatchEvent(new Event('input'));

      expect(mockListener).toHaveBeenCalledWith({ id: '123', name: 'edited parser name' });
    });
  });

});
