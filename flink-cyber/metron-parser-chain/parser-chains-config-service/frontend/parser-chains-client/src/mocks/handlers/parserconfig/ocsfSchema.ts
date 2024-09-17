import {rest} from 'msw';
import ocsfSchemaJson from '../../../test/resources/parserconfig/ocsf-schema.json';
import {OcsfSchemaModel} from "../../../app/chain-page/components/ocsf-form/ocsf-form.model";

const ocsfSchema = ocsfSchemaJson as OcsfSchemaModel;
if (localStorage.getItem('ocsfSchema') === null) {
    localStorage.setItem('ocsfSchema', JSON.stringify(ocsfSchema));
}

export const ocsfSchemaHandlers = [
    rest.get<OcsfSchemaModel>('/api/v1/ocsf', (req, res, ctx) => {
        const pt: OcsfSchemaModel = JSON.parse(localStorage.getItem("ocsfSchema"));
        return res(
            ctx.status(200),
            ctx.json(pt)
        );
    }),
];
