import {Measure, MeasureReport} from "./fhir";

export class ReportModel {
    identifier: string
    bundleId: string;
    version: string;
    status: string;
    date: string;
    measure: Measure;
    measureReport: MeasureReport;
}
