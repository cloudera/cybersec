import {rest} from 'msw';
import {ChainDetailsModel} from "../../../app/chain-page/chain-page.models";
import {v1 as uuidv1} from 'uuid';
import chainsJson from "../../../test/resources/parserconfig/chains.json";

const chains = chainsJson as ChainDetailsModel[];
if (localStorage.getItem('chains') === null) {
  localStorage.setItem('chains', JSON.stringify(chains));
}

export const chainHandlers = [
  rest.get<ChainDetailsModel[]>('/api/v1/parserconfig/chains', (req, res, ctx) => {
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    return res(
      ctx.status(200),
      ctx.json(storedChains)
    );
  }),
  rest.get<ChainDetailsModel, { chainId: string }>('/api/v1/parserconfig/chains/:chainId', (req, res, ctx) => {
    const {chainId} = req.params;
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    const chain = storedChains.find(chain => chain.id === chainId);
    if (chain) {
      return res(
        ctx.status(200),
        ctx.json(chain)
      );
    } else {
      return res(
        ctx.status(404)
      )
    }
  }),
  rest.post<ChainDetailsModel, any, ChainDetailsModel>('/api/v1/parserconfig/chains', (req, res, ctx) => {
    const reqChain = req.body;
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    if (storedChains.find(chain => chain.name === reqChain.name)) {
      return res(
        ctx.status(409)
      )
    }
    const newChain = {
      ...reqChain,
      id: uuidv1()
    }
    localStorage.setItem('chains', JSON.stringify([...storedChains, newChain]));
    return res(
      ctx.status(200),
      ctx.json(newChain)
    );
  }),
  rest.put<ChainDetailsModel, { chainId: string }, ChainDetailsModel>('/api/v1/parserconfig/chains/:chainId', (req, res, ctx) => {
    const {chainId} = req.params;
    const reqChain: ChainDetailsModel = req.body;
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    const chain: ChainDetailsModel = storedChains.find(chain => chain.id === chainId);
    if (chain === null) {
      return res(
        ctx.status(409)
      )
    }
    const newChains: ChainDetailsModel[] = storedChains.map(chain => chain.id === chainId ? reqChain : chain);

    localStorage.setItem('chains', JSON.stringify(newChains));
    return res(
      ctx.status(204),
    );
  }),
  rest.delete<ChainDetailsModel, any, ChainDetailsModel>('/api/v1/parserconfig/chains/:chainId', (req, res, ctx) => {
    const {chainId} = req.params;
    const storedChains: ChainDetailsModel[] = JSON.parse(localStorage.getItem("chains"));
    const newChains = storedChains.filter(chain => chain.id !== chainId);
    localStorage.setItem("chains", JSON.stringify(newChains));
    return res(
      ctx.status(200),
    );
  }),
];
