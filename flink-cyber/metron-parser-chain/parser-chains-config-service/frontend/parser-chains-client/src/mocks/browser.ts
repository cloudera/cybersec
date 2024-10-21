import {setupWorker} from "msw";
import {chainHandlers} from "./handlers/parserconfig/chainHandlers";
import {customFormHandlers} from './handlers/parserconfig/customFormHandlers'
import {parserTypesHandlers} from "./handlers/parserconfig/parserTypes";
import {chainParsersHandlers} from "./handlers/parserconfig/chainParsersHandlers";
import {clusterHandlers} from "./handlers/clusterHandlers";
import {ocsfSchemaHandlers} from "./handlers/parserconfig/ocsfSchema";


const worker = setupWorker(...chainHandlers, ...customFormHandlers, ...parserTypesHandlers, ...chainParsersHandlers, ...clusterHandlers, ...ocsfSchemaHandlers);

export {worker}
