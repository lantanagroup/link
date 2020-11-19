import {Component, Input, OnInit} from '@angular/core';
import {IReportPlugin} from "../report-plugin";
import {QueryReport} from "../../model/query-report";

@Component({
  selector: 'app-measure-eval',
  templateUrl: './measure-eval.component.html',
  styleUrls: ['./measure-eval.component.css']
})
export class MeasureEvalComponent implements OnInit, IReportPlugin {
  @Input() report: QueryReport;

  constructor() { }

  ngOnInit(): void {
  }

  refreshed() {
  }

}
