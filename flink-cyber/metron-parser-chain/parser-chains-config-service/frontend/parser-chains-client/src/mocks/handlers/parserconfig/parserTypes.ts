import {rest} from 'msw';
import parserTypesJson from '../../../test/resources/parserconfig/parser-types.json';
import {ChainDetailsModel} from "../../../app/chain-page/chain-page.models";

interface ParserTypes {
  id: string,
  name: string
}

const parserTypes = parserTypesJson as ParserTypes[];
if (localStorage.getItem('parserTypes') === null) {
  localStorage.setItem('parserTypes', JSON.stringify(parserTypes));
}

export const parserTypesHandlers = [
  rest.get<ChainDetailsModel[]>('/api/v1/parserconfig/parser-types', (req, res, ctx) => {
    const parserTypes: ParserTypes[] = JSON.parse(localStorage.getItem("parserTypes"));
    return res(
      ctx.status(200),
      ctx.json(parserTypes)
    );
  }),
];
