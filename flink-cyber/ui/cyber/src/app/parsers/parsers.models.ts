export interface ParserChainModel {
    id: string;
    name: string;
    parsers: string[] | ParserModel[];
}

export interface PartialParserChainModel {
    id?: string;
    name?: string;
    parsers?: string[] | ParserModel[];
}

export interface RouteModel {
    id: string;
    name: string;
    default: boolean;
    subchain: string | ParserChainModel;
    matchingValue?: string;
}

export interface PartialRouteModel {
    id?: string;
    name?: string;
    default?: boolean;
    subchain?: string | ParserChainModel;
    matchingValue?: string;
}

export interface ParserModel {
    id: string;
    type: string;
    name: string;
    config?: any;
    routing?: any;
}

export interface PartialParserModel {
    id?: string;
    type?: string;
    name?: string;
    config?: any;
    routing?: any;
}

export interface ChainDetailsModel {
    id: string;
    name: string;
    parsers: ParserModel[];
}
