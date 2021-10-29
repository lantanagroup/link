import {Component, EventEmitter, Output} from '@angular/core';
import {NgbDate, NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'ngbd-datepicker-popup',
  templateUrl: './datepicker-popup.html',
  styles: [`
      .form-inline .input-group {
          width: 12rem;
      }
  `]
})

export class NgbdDatepickerPopup {
  model: NgbDateStruct;
  @Output() change: EventEmitter<any> = new EventEmitter<any>();

  onDateSelection(date: NgbDate) {
    this.model = date;
    this.change.emit(this.model);
  }

  onDateEntered() {
    this.change.emit(this.model)
  }

  public resetDate() {
    this.model = null;
  }
}
