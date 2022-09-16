export function normalizeParserConfig(config, normalized?) {
  normalized = normalized || {
    parsers: {},
    chains: {},
    routes: {}
  };
  normalized.chains[config.id] = {
    ...config,
    parsers: (config.parsers || []).map((parser) => {
      normalized.parsers[parser.id] = { ...parser };
      if (parser.type === 'Router' && parser.routing && parser.routing.routes) {
        normalized.parsers[parser.id].routing = {
          ...parser.routing,
          routes: parser.routing.routes.map((route) => {
            normalized.routes[route.id] = { ...route };
            if (route.subchain) {
              const chainId = route.subchain.id;
              normalizeParserConfig(route.subchain, normalized);
              normalized.routes[route.id].subchain = chainId;
            }
            return route.id;
          })
        };
      }
      return parser.id;
    })
  };
  return normalized;
}

export function denormalizeParserConfig(chain, config) {
  const denormalized = {
    ...chain
  };
  if (denormalized.parsers) {
    denormalized.parsers = chain.parsers.map((parserId) => {
      const parser = {
        ...config.parsers[parserId]
      };
      if (parser.type === 'Router') {
        parser.routing = {
          ...parser.routing,
          routes: (parser.routing.routes || []).map((routeId) => {
            const route = {
              ...config.routes[routeId]
            };
            const subchainId = route.subchain;
            if (subchainId) {
              const subchain = config.chains[subchainId];
              route.subchain = denormalizeParserConfig(subchain, config);
            }
            return route;
          })
        };
      }
      return parser;
    });
  }
  return denormalized;
}
