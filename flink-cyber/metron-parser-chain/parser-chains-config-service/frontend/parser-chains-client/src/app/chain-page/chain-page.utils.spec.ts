import {
  denormalizeParserConfig as denormalize,
  normalizeParserConfig as normalize
} from './chain-page.utils';

describe('Chain page utils: normalize', () => {
  it('should normalize the parser config', () => {
    const parserConfig = {
      id: '1',
      name: 'chain 1',
      parsers: [{
        id: '1',
        name: 'parser 1'
      }, {
        id: '2',
        name: 'parser 2',
        type: 'Router',
        routing: {
          routes: [{
            id: '1',
            name: 'route 1',
            subchain: {
              id: '2',
              name: 'chain 2',
              parsers: [{
                id: '3',
                name: 'parser 3'
              }, {
                id: '4',
                name: 'parser 4',
                type: 'Router',
                routing: {
                  routes: [{
                    id: '3',
                    name: 'route 3',
                    subchain: {
                      id: '3',
                      name: 'chain 3',
                      parsers: [{
                        id: '5',
                        name: 'parser 5'
                      }]
                    }
                  }]
                }
              }]
            }
          }, {
            id: '2',
            name: 'route 2',
            subchain: {
              id: '4',
              name: 'chain 4',
              parsers: [{
                id: '6',
                name: 'parser 6'
              }, {
                id: '7',
                name: 'parser 7'
              }]
            }
          }]
        }
      }]
    };

    const normalized = normalize(parserConfig);

    expect(normalized).toEqual({
      routes: {
        1: {
          id: '1',
          name: 'route 1',
          subchain: '2'
        },
        2: {
          id: '2',
          name: 'route 2',
          subchain: '4'
        },
        3: {
          id: '3',
          name: 'route 3',
          subchain: '3'
        }
      },
      parsers: {
        1: {
          id: '1',
          name: 'parser 1'
        },
        2: {
          id: '2',
          name: 'parser 2',
          type: 'Router',
          routing: {
            routes: ['1', '2']
          }
        },
        3: {
          id: '3',
          name: 'parser 3'
        },
        4: {
          id: '4',
          name: 'parser 4',
          type: 'Router',
          routing: {
            routes: ['3']
          }
        },
        5: {
          id: '5',
          name: 'parser 5'
        },
        6: {
          id: '6',
          name: 'parser 6'
        },
        7: {
          id: '7',
          name: 'parser 7'
        }
      },
      chains: {
        1: {
          id: '1',
          name: 'chain 1',
          parsers: ['1', '2']
        },
        2: {
          id: '2',
          name: 'chain 2',
          parsers: ['3', '4']
        },
        3: {
          id: '3',
          name: 'chain 3',
          parsers: ['5']
        },
        4: {
          id: '4',
          name: 'chain 4',
          parsers: ['6', '7']
        }
      }
    });
  });

  it('should denormalize normalized parser config', () => {
    const parserConfig = {
      id: '1',
      name: 'chain 1',
      parsers: [{
        id: '1',
        name: 'parser 1'
      }, {
        id: '2',
        name: 'parser 2',
        type: 'Router',
        routing: {
          routes: [{
            id: '1',
            name: 'route 1',
            subchain: {
              id: '2',
              name: 'chain 2',
              parsers: [{
                id: '3',
                name: 'parser 3'
              }, {
                id: '4',
                name: 'parser 4',
                type: 'Router',
                routing: {
                  routes: [{
                    id: '3',
                    name: 'route 3',
                    subchain: {
                      id: '3',
                      name: 'chain 3',
                      parsers: [{
                        id: '5',
                        name: 'parser 5'
                      }]
                    }
                  }]
                }
              }]
            }
          }, {
            id: '2',
            name: 'route 2',
            subchain: {
              id: '4',
              name: 'chain 4',
              parsers: [{
                id: '6',
                name: 'parser 6'
              }, {
                id: '7',
                name: 'parser 7'
              }]
            }
          }]
        }
      }]
    };
    const mainChainId = '1';
    const normalized = normalize(parserConfig);
    const mainChain = normalized.chains[mainChainId];
    const denormalized = denormalize(mainChain, normalized);
    expect(denormalized).toEqual(parserConfig);
  });
});
