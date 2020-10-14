import {Component, Input, OnInit} from '@angular/core';
import {QueryReport} from '../../model/query-report';
import {IReportPlugin} from '../report-plugin';

@Component({
  templateUrl: './pihc-report.component.html',
  styleUrls: ['./pihc-report.component.css']
})
export class PihcReportComponent implements OnInit, IReportPlugin {
  @Input() report: QueryReport;

  constructor() { }

  ngOnInit(): void {
    this.report.questions = [
      'facilityId',
      'summaryCensusId',
      'numc19confnewadm',
      'numc19suspnewadm',
      'numc19honewpats',
      'numConfC19HONewPats',
      'numC19HospPats',
      'numConfC19HospPats',
      'numC19MechVentPats',
      'numConfC19MechVentPats',
      'numC19HOPats',
      'numConfC19HOPats',
      'numC19OverflowPats',
      'numConfC19OverflowPats',
      'numC19OFMechVentPats',
      'numConfC19OFMechVentPats',
      'numc19prevdied',
      'numConfC19PrevDied',
      'numTotBeds',
      'numBeds',
      'numBedsOcc',
      'numICUBeds',
      'numNICUBeds',
      'numICUBedsOcc',
      'mechanicalVentilators',
      'mechanicalVentilatorsUsed'];
  }

  refreshed() {
  }
}
