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

const router = require('express').Router();
const crypto = require('crypto');
const uuid = require('uuid/v1');
const debug = require('debug');

const log = debug('tmo-config');

const GET = router.get.bind(router);
const PUT = router.put.bind(router);
const POST = router.post.bind(router);
const DELETE = router.delete.bind(router);

let chains = require('./mock-data/chains.json');
let parserTypes = require('./mock-data/parser-types.json');
let formConfigs = require('./mock-data/form-configs.json');

GET('/chains', getChains);
POST('/chains', createChain);
PUT('/chains/:id', updateChain);
DELETE('/chains/:id', deleteChain);
GET('/chains/:id', getChain);

GET('/chains/:id/parsers', getParsers);
POST('/chains/:id/parsers', addParser);

GET('/parser-types', getParserTypes);

POST('/tests', createParseJob);

GET('/parser-form-configuration/:type', getFormConfig);
GET('/parser-form-configuration', getFormConfigs);

function createParseJob(req, res) {
  const parsers = req.body.chainConfig.parsers
  const sources = req.body.sampleData.source;
  const log = ['PASS', 'FAIL'];
  let results = sources.map(source => { return {
      input: source,
      output: {
        original_string: source
      },
      log: {}
    }
  });


  results.map((result, index) => {
    const asaTagRegex = /%ASA-\d\-\d*\b/g
    const asaMessageRegex = /(?<=%ASA-\d\-\d*\:)(.*)/g
    const syslogMessageRegex = /%ASA-\d\-\d*\:(.*)/g


    result.output.ASA_TAG = asaTagRegex.exec(result.input);
    result.output.ASA_message = asaMessageRegex.exec(result.input);
    if (log[Math.floor(Math.random() * 2)] === 'PASS') {
      result.output.syslogMessage = syslogMessageRegex.exec(result.input);
      result.log = {
        "type": "info",
        "message": "Parsing Successful",
        "parserId": parsers[index].id
      };
    } else {
      result.log = {
        "type": "error",
        "message": `Parsing Failed: ${parsers[index].name} parser unable to parse.`,
        "parserId": parsers[index].id
      };
    }
  });

  let response = {
    results
  };

  setTimeout(() => {
    res.status(200).send(response);
  }, 1000);
}

function getChains(req, res) {
  res.status(200).send(chains);
}

function getChain(req, res) {
  const chainId = req.params.id;
  const chain = chains.find((ch) => ch.id === chainId);
  if (chain) {
    res.status(200).send(chain);
    return;
  }
  res.status(404).send();
}

function createChain(req, res) {
  const id = crypto.randomBytes(8).toString('hex');
  const newChain = {
    ...req.body,
    id: id,
    parsers: req.body.parsers || []
  }

  if (chains.find(chain => chain.name === newChain.name)) {
    res.status(409).send("This parser chain name already exists");
    return;
  }

  chains = [
    ...chains,
    newChain
  ]

  res.status(201).send(newChain);
}

function updateChain(req, res) {
  const id = req.params.id;
  const chainIndex = chains.findIndex(chain => chain.id === id);
  if (chainIndex > -1) {
    chains[chainIndex] = {
      ...chains[chainIndex],
      ...req.body
    };
    res.status(204).send();
    return;
  }
  res.status(404).send();
}

function deleteChain(req, res) {
  const id = req.params.id;
  if (chains.filter(chain => chain.id === id)) {
    chains = chains.filter(chain => chain.id !== id);
    res.status(200).send(chains);
    return;
  }
  res.status(404).send();
}

function getParsers(req, res) {
  const id = req.params.id;
  const chain = chains.find(chain => chain.id === id);
  if (chain) {
    res.status(200).send(chain.parsers || []);
    return;
  }
  res.status(404).send();
}

function addParser(req, res) {
  const id = req.params.id;
  const parser = req.body;
  const chain = chains.find(chain => chain.id === id);
  if (chain) {
    if (!chain.parsers) {
      chain.parsers = [];
    }
    parser.id = uuid();
    chain.parsers.push(parser);
    res.status(200).send(parser);
    return;
  }
  res.status(404).send();
}

function getParserTypes(req, res) {
  return res.status(200).send(parserTypes);
}

function getFormConfig(req, res) {
  const type = req.params.type;
  const config = formConfigs[type];
  if (config) {
    res.status(200).send(config);
    return;
  }
  res.status(404).send();
}

function getFormConfigs(req, res) {
  res.status(200).send(formConfigs);
}

module.exports = router;
