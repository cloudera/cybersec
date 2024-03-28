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

import * as fromAddParserActions from '../chain-add-parser-page/chain-add-parser-page.actions';

import * as fromActions from './chain-page.actions';
import * as fromReducers from './chain-page.reducers';


describe('chain-page: reducers', () => {

  it('should remove a selected parser from the store', () => {
    const state: fromReducers.ChainPageState = {
      chains: {
        4533: {
          id: '4533',
          name: 'test chain a',
          parsers: ['123', '456']
        }
      },
      parsers: {
        123: {
          id: '123',
          name: 'Syslog',
          type: 'Grok',
          config: {},
        },
        456: {
          id: '456',
          name: 'Asa',
          type: 'Grok',
          config: {},
        }
      },
      dirtyParsers: [],
      dirtyChains: [],
      routes: {},
      path: [],
      error: '',
      parserToBeInvestigated: '',
      failedParser: '',
      indexMappings: {path: '', result: {}}
    };
    expect(
      fromReducers.reducer(state, new fromActions.RemoveParserAction({
        id: '456',
        chainId: '4533'
      }))
    ).toEqual({
      chains: {
        4533: {
          id: '4533',
          name: 'test chain a',
          parsers: ['123']
        }
      },
      parsers: {
        123: {
          id: '123',
          name: 'Syslog',
          type: 'Grok',
          config: {},
        }
      },
      routes: {},
      parserToBeInvestigated: '',
      failedParser: '',
      dirtyParsers: [],
      dirtyChains: ['4533'],
      path: [],
      indexMappings: {path: '', result: {}},
      error: ''
    });
  });

  it('should add a parser', () => {
    const state = {
      chains: {
        456: {
          id: '456',
          name: 'test chain a',
          parsers: []
        }
      },
      parsers: {},
      dirtyParsers: [],
      dirtyChains: [],
      routes: {},
      path: [],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };
    const newState = fromReducers.reducer(state, new fromAddParserActions.AddParserAction({
      chainId: '456',
      parser: {
        id: '123',
        name: 'parser 1',
        type: 'foo'
      }
    }));
    expect(newState.parsers['123']).toEqual({
      id: '123',
      name: 'parser 1',
      type: 'foo'
    });
    expect(newState.chains['456'].parsers).toEqual(['123']);
    expect(newState.dirtyChains).toEqual(['456']);
    expect(newState.dirtyParsers).toEqual(['123']);
  });

  it('load success: should set all the components', () => {
    const state = {
      chains: null,
      parsers: null,
      routes: null,
      parserToBeInvestigated: '',
      failedParser: '',
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      indexMappings: {path: '', result: {}},
      error: ''
    };
    const chains = {};
    const parsers = {};
    const routes = {};
    const newState = fromReducers.reducer(state, new fromActions.LoadChainDetailsSuccessAction({
      chainId: '123',
      chains,
      parsers,
      routes
    }));
    expect(newState.chains).toBe(chains);
    expect(newState.parsers).toBe(parsers);
    expect(newState.routes).toBe(routes);
  });

  it('should update the given parser', () => {
    const state = {
      chains: null,
      parsers: {
        456: {
          id: '456',
          type: 'grok',
          name: 'some parser',
          config: 'old'
        }
      },
      routes: null,
      parserToBeInvestigated: '',
      failedParser: '',
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      indexMappings: {path: '', result: {}},
      error: ''
    };
    const newState = fromReducers.reducer(state, new fromActions.UpdateParserAction({
      chainId: '123',
      parser: {
        id: '456',
        config: 'new'
      }
    }));
    expect(newState.parsers['456']).toEqual({
      id: '456',
      type: 'grok',
      name: 'some parser',
      config: 'new'
    });
    expect(newState.dirtyParsers).toEqual(['456']);
    expect(newState.dirtyChains).toEqual(['123']);
  });

  it('should update the given chain name', () => {
    const state = {
      parsers: null,
      chains: {
        456: {
          id: '456',
          name: 'old',
          parsers: []
        }
      },
      routes: null,
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };
    const newState = fromReducers.reducer(state, new fromActions.UpdateChainAction({
      chain: {
        id: '456',
        name: 'new',
        parsers: []
      }
    }));
    expect(newState.chains['456']).toEqual({
      id: '456',
      name: 'new',
      parsers: []
    });
    expect(newState.dirtyChains).toEqual(['456']);
  });

  it('should add a chain', () => {
    const state = {
      chains: {},
      parsers: {},
      dirtyParsers: [],
      dirtyChains: [],
      routes: {},
      path: [],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };
    const newState = fromReducers.reducer(state, new fromActions.AddChainAction({
      chain: {
        id: '456',
        name: 'chain 1',
        parsers: []
      }
    }));
    expect(newState.chains['456']).toEqual({
      id: '456',
      name: 'chain 1',
      parsers: []
    });
  });

  it('should return with the desired parser', () => {
    const desiredParser = {
      id: '456',
      type: 'grok',
      name: 'some parser',
      config: 'old'
    };
    const state = {
      'chain-page': {
        chains: null,
        parsers: {
          456: desiredParser
        },
        routes: null,
        error: ''
      }
    };

    const parser = fromReducers.getParser({id: '456'})(state);
    expect(parser).toBe(desiredParser);
  });

  it('should return with the desired chain', () => {
    const desiredChain = {
      id: '456',
      name: 'some chain',
      parsers: []
    };
    const state = {
      'chain-page': {
        chains: {
          456: desiredChain
        },
        parsers: null,
        routes: null,
        error: ''
      }
    };
    const chain = fromReducers.getChain({id: '456'})(state);
    expect(chain).toBe(desiredChain);
  });

  it('should return with the desired route', () => {
    const desiredRoute = {
      id: '456',
      name: 'some route',
      subchain: '',
      default: false,
    };
    const state = {
      'chain-page': {
        chains: null,
        parsers: null,
        routes: {
          456: desiredRoute
        },
        error: ''
      }
    };
    const route = fromReducers.getRoute({id: '456'})(state);
    expect(route).toBe(desiredRoute);
  });

  it('should return with the currently investigated parser', () => {
    const stateForSelector = {
      'chain-page': {
        parserToBeInvestigated: '4321'
      }
    };

    const investigatedParserSelector = fromReducers.getParserToBeInvestigated(stateForSelector);
    expect(investigatedParserSelector).toBeDefined('4321');
  });

  it('should add an investigated parser to the store', () => {
    const state = {
      chains: null,
      parsers: null,
      dirtyParsers: [],
      dirtyChains: [],
      routes: {},
      path: [],
      error: '',
      parserToBeInvestigated: '1234',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };

    const investigatedParserReducer = fromReducers.reducer(state, new fromActions.InvestigateParserAction({id: '1234'}));
    expect(investigatedParserReducer.parserToBeInvestigated).toBe('1234');
  });

  it('should return with the different levels the user is in represented by real chain objects', () => {
    const chain1 = {id: '1', name: 'a', parsers: []};
    const chain2 = {id: '2', name: 'b', parsers: []};
    const chain3 = {id: '3', name: 'c', parsers: []};
    const state = {
      'chain-page': {
        chains: {
          1: chain1,
          2: chain2,
          3: chain3,
        },
        path: ['2', '1', '3']
      }
    };

    const pathWithChains = fromReducers.getPathWithChains(state);
    expect(pathWithChains).toEqual([
      chain2, chain1, chain3
    ]);
  });

  it('should return with the path the user is currently following', () => {
    const state = {
      'chain-page': {
        path: ['2', '1', '3']
      }
    };

    const path = fromReducers.getPath(state);
    expect(path).toEqual(['2', '1', '3']);
  });

  it('should return with all the form configs', () => {
    const formConfigs = {};
    const state = {
      'chain-page': {
        formConfigs
      }
    };

    const pathWithChains = fromReducers.getFormConfigs(state);
    expect(pathWithChains).toBe(formConfigs);
  });

  it('should return with a form config for a certain parser type', () => {
    const formConfigs = {
      foo: null
    };
    const state = {
      'chain-page': {
        formConfigs
      }
    };

    const config = fromReducers.getFormConfigByType({type: 'foo'})(state);
    expect(config).toBeNull();
  });

  it('should return with the dirty chains', () => {
    const state = {
      'chain-page': {
        dirtyChains: ['1', '3', '2']
      }
    };

    const dirtyChains = fromReducers.getDirtyChains(state);
    expect(dirtyChains).toEqual(['1', '3', '2']);
  });

  it('should return with the dirty parsers', () => {
    const state = {
      'chain-page': {
        dirtyParsers: ['1', '3', '2']
      }
    };

    const dirtyParsers = fromReducers.getDirtyParsers(state);
    expect(dirtyParsers).toEqual(['1', '3', '2']);
  });

  it('should return with the dirty chains and the dirty parsers', () => {
    const state = {
      'chain-page': {
        dirtyChains: ['1', '3', '2'],
        dirtyParsers: ['1', '3', '2']
      }
    };
    const dirtyStuff = fromReducers.getDirtyStatus(state);
    expect(dirtyStuff).toEqual({
      dirtyChains: ['1', '3', '2'],
      dirtyParsers: ['1', '3', '2']
    });
  });

  it('should remove a chain id from the path', () => {
    const state = {
      chains: null,
      parsers: null,
      dirtyParsers: [],
      dirtyChains: [],
      routes: {},
      path: ['123', '456', '678'],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.RemoveFromPathAction({
      chainId: ['123', '678']
    }));
    expect(newState.path).toEqual(['456']);
  });

  it('should add a chain id to the path', () => {
    const state = {
      chains: null,
      parsers: null,
      dirtyParsers: [],
      dirtyChains: [],
      routes: {},
      path: ['123', '456', '678'],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.AddToPathAction({
      chainId: '91011'
    }));
    expect(newState.path).toEqual(['123', '456', '678', '91011']);
  });

  it('should remove a route', () => {
    const state = {
      chains: {
        678: {
          id: '678',
          name: 'chain 1',
          parsers: ['123']
        }
      },
      parsers: {
        123: {
          id: '123',
          name: 'parser 1',
          type: 'foo',
          routing: {
            routes: ['456']
          }
        }
      },
      routes: {
        456: {
          id: '456',
          name: 'route 1',
          matchingValue: 'foo',
          subchain: null,
          default: false,
        }
      },
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      indexMappings: {path: '', result: {}},
      parserToBeInvestigated: '',
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.RemoveRouteAction({
      chainId: '678',
      parserId: '123',
      routeId: '456'
    }));
    expect(newState.routes).toEqual({});
    expect(newState.dirtyChains).toEqual(['678']);
    expect(newState.dirtyParsers).toEqual(['123']);
    expect(newState.parsers['123'].routing.routes).toEqual([]);
  });

  it('should add a route', () => {
    const state = {
      chains: {
        678: {
          id: '678',
          name: 'chain 1',
          parsers: ['123']
        }
      },
      parsers: {
        123: {
          id: '123',
          name: 'parser 1',
          type: 'foo',
          routing: {
            routes: []
          }
        }
      },
      routes: {},
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      indexMappings: {path: '', result: {}},
      parserToBeInvestigated: '',
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.AddRouteAction({
      chainId: '678',
      parserId: '123',
      route: {
        id: '456',
        name: 'route 1',
        matchingValue: 'foo',
        subchain: null,
        default: false,
      }
    }));
    expect(newState.routes).toEqual({
      456: {
        id: '456',
        name: 'route 1',
        matchingValue: 'foo',
        subchain: null,
        default: false,
      }
    });
    expect(newState.dirtyChains).toEqual(['678']);
    expect(newState.dirtyParsers).toEqual(['123']);
    expect(newState.parsers['123'].routing.routes).toEqual(['456']);
  });

  it('should update a route', () => {
    const state = {
      chains: {
        678: {
          id: '678',
          name: 'chain 1',
          parsers: ['123']
        }
      },
      parsers: {
        123: {
          id: '123',
          name: 'parser 1',
          type: 'foo',
          routing: {
            routes: []
          }
        }
      },
      routes: {
        456: {
          id: '456',
          name: 'route 1',
          matchingValue: 'foo',
          subchain: null,
          default: false,
        }
      },
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      indexMappings: {path: '', result: {}},
      parserToBeInvestigated: '',
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.UpdateRouteAction({
      chainId: '678',
      parserId: '123',
      route: {
        id: '456',
        name: 'route 1 UPDATED',
      }
    }));
    expect(newState.routes).toEqual({
      456: {
        id: '456',
        name: 'route 1 UPDATED',
        matchingValue: 'foo',
        subchain: null,
        default: false,
      }
    });
    expect(newState.dirtyChains).toEqual(['678']);
    expect(newState.dirtyParsers).toEqual(['123']);
  });

  it('should add a form config by type', () => {
    const state = {
      chains: {},
      parsers: {},
      routes: {},
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.GetFormConfigSuccessAction({
      parserType: 'foo',
      formConfig: {
        id: '1',
        name: 'field',
        schemaItems: null
      }
    }));
    expect(newState.formConfigs).toEqual({
      foo: {
        id: '1',
        name: 'field',
        schemaItems: null
      }
    });
  });

  it('should set the form configs', () => {
    const state = {
      chains: {},
      parsers: {},
      routes: {},
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      indexMappings: {path: '', result: {}},
      parserToBeInvestigated: '',
      failedParser: '',
    };

    const newState = fromReducers.reducer(state, new fromActions.GetFormConfigsSuccessAction({
      formConfigs: {
        foo: {
          id: '1',
          name: 'field',
          schemaItems: null
        }
      }
    }));
    expect(newState.formConfigs).toEqual({
      foo: {
        id: '1',
        name: 'field',
        schemaItems: null
      }
    });
  });

  it('save parsers should reset dirty chains and parsers', () => {
    const state = {
      chains: {},
      parsers: {},
      routes: {},
      dirtyParsers: ['123'],
      dirtyChains: ['456'],
      path: [],
      error: '',
      parserToBeInvestigated: '',
      indexMappings: {path: '', result: {}},
      failedParser: '',
    };
    const newState = fromReducers.reducer(state, new fromActions.SaveParserConfigAction({chainId: '123'}));
    expect(newState.dirtyChains).toEqual([]);
    expect(newState.dirtyParsers).toEqual([]);
  });

  it('set the default route (only one is allowed)', () => {
    const state = {
      chains: {
        123: {
          id: '123',
          name: 'chain',
          parsers: ['456']
        }
      },
      parsers: {
        456: {
          id: '456',
          name: 'parser',
          type: 'foo'
        }
      },
      routes: {
        678: {
          id: '678',
          name: 'route',
          default: false,
          subchain: '444'
        },
        91011: {
          id: '91011',
          name: 'route 2',
          default: true,
          subchain: '555'
        }
      },
      dirtyParsers: [],
      dirtyChains: [],
      path: [],
      error: '',
      indexMappings: {path: '', result: {}},
      parserToBeInvestigated: '',
      failedParser: '',
    };
    const newState = fromReducers.reducer(state,
      new fromActions.SetRouteAsDefaultAction({
        chainId: '123',
        parserId: '456',
        routeId: '678'
      })
    );
    expect(newState.dirtyChains).toEqual(['123']);
    expect(newState.dirtyParsers).toEqual(['456']);
    expect(newState.routes['91011'].default).toBe(false);
    expect(newState.routes['678'].default).toBe(true);
  });
});

describe('chain page reducer utils: uniqueAdd', () => {
  it('should add an item and keep only the latest in the list', () => {
    expect(
      fromReducers.uniqueAdd(['1', '2', '3'], '2')
    ).toEqual(['1', '3', '2']);
  });
});
