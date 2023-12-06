import { LinkInterface } from "../interfaces/globals.interface";

export interface ResourceSection {
    name: string;
    links: LinkInterface[];
}

export interface ResourceGroup {
    name: string;
    sections: ResourceSection[];
}