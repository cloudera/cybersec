import {rest} from 'msw';
import {ChainDetailsModel} from "../../../app/chain-page/chain-page.models";
import formConfigJson from "../../../test/resources/parserconfig/form-configs.json";
import {CustomFormConfig} from "../../../app/chain-page/components/custom-form/custom-form.component";


export interface CustomForms {
  [key: string]: CustomForm;
}

export interface CustomForm {
  id: string;
  name: string;
  schemaItems: CustomFormConfig[];
}

const formConfigs = formConfigJson as CustomForms;

if (localStorage.getItem('formConfigs') === null) {
  localStorage.setItem('formConfigs', JSON.stringify(formConfigs));
}


export const customFormHandlers = [
  rest.get<ChainDetailsModel[]>('/api/v1/parserconfig/parser-form-configuration', (req, res, ctx) => {
    const customFormConfigs: CustomForm = JSON.parse(localStorage.getItem('formConfigs'));
    return res(
      ctx.status(200),
      ctx.json(customFormConfigs)
    );
  }),
  rest.get<ChainDetailsModel, { type: string }>('/api/v1/parserconfig/parser-form-configuration/:type', (req, res, ctx) => {
    const {type} = req.params;
    const customFormConfigs: CustomForm = JSON.parse(localStorage.getItem('formConfigs'));
    const formConfig = customFormConfigs[type]

    if (formConfig) {
      return res(
        ctx.status(200),
        ctx.json(formConfig)
      );
    } else {
      return res(
        ctx.status(404)
      )
    }
  })
];
