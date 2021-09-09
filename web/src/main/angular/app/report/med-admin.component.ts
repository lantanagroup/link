import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-report-med-admin',
  templateUrl: './med-admin.component.html',
  styleUrls: ['./med-admin.component.css']
})
export class MedAdminComponent implements OnInit, OnDestroy {

  @Input() report: any;
  @Output() invalidate: EventEmitter<any> = new EventEmitter<any>();

  setRequiredErrorsFlag(hasErrors) {
    this.invalidate.emit(hasErrors);
  }

  ngOnInit() {

  }

  ngOnDestroy() {

  }
}
