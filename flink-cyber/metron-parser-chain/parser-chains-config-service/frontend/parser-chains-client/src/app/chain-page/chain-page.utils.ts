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
