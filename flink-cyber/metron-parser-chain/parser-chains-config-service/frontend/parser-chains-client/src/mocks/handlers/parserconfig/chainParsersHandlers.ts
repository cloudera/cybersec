import {rest} from 'msw';
import {ChainDetailsModel, ParserChainModel, ParserModel} from "../../../app/chain-page/chain-page.models";
import {v1 as uuidv1} from 'uuid';
import chainsJson from "../../../test/resources/parserconfig/chains.json";

const chains = chainsJson as ChainDetailsModel[];
if (localStorage.getItem('chains') === null) {
  localStorage.setItem('chains', JSON.stringify(chains));
}

export const chainParsersHandlers = [
  rest.get<ParserChainModel[], { chainId: string }>('/api/v1/parserconfig/chains/:chainId/parsers', (req, res, ctx) => {
    const {chainId} = req.params;
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    const chain: ChainDetailsModel = storedChains.find(chain => chain.id === chainId);
    if (chain) {
      return res(
        ctx.status(200),
        ctx.json(chain.parsers)
      );
    } else {
      return res(
        ctx.status(404)
      )
    }
  }),
  rest.post<ParserModel, { chainId: string }, ParserModel[]>('/api/v1/parserconfig/chains/:chainId/parsers', (req, res, ctx) => {
    const parser: ParserModel = req.body;
    const {chainId} = req.params;
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    const chain: ChainDetailsModel = storedChains.find(chain => chain.id === chainId);
    if (!chain) {
      return res(
        ctx.status(404)
      )
    }
    const newParsers: ParserModel[] = [...chain.parsers, {...parser, id: uuidv1()}];
    const newChains: ChainDetailsModel[] = storedChains.map(chain => chain.id === chainId ? {
      ...chain,
      parsers: newParsers
    } : chain);
    localStorage.setItem('chains', JSON.stringify(newChains));
    return res(
      ctx.status(200),
      ctx.json(newParsers)
    );
  }),
];
