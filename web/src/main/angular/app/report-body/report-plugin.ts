import {QueryReport} from '../model/query-report';

export interface IReportPlugin {
  report: QueryReport;
  refreshed();
}