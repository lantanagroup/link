import {Measure, MeasureReport} from "./fhir";

export class ReportModel {
    version: string;
    status: string;
    date: string;
    reportMeasureList: {
        identifier: string
        bundleId: string;
        measure: Measure;
        measureReport: MeasureReport;
    }[];
}
