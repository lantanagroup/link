import {Component, EventEmitter, Input, Output} from '@angular/core';
import {NgbCalendar, NgbDate, NgbDateParserFormatter} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'ngbd-datepicker-range-popup',
    templateUrl: './datepicker-range-popup.html',
    styles: [`
        .form-group.hidden {
            width: 0;
            margin: 0;
            border: none;
            padding: 0;
        }

        .custom-day {
            text-align: center;
            padding: 0.185rem 0.25rem;
            display: inline-block;
            height: 2rem;
            width: 2rem;
        }

        .custom-day.focused {
            background-color: #e6e6e6;
    }
    .custom-day.range, .custom-day:hover {
      background-color: rgb(2, 117, 216);
      color: white;
    }
    .custom-day.faded {
      background-color: rgba(2, 117, 216, 0.5);
    }
  `]
})
export class NgbdDatepickerRangePopup {

    hoveredDate: NgbDate | null = null;
    @Output() change: EventEmitter<any> = new EventEmitter<any>();
    @Input() fromDate: NgbDate | null;
    @Input() toDate: NgbDate | null;

    constructor(private calendar: NgbCalendar, public formatter: NgbDateParserFormatter) {
    }

    onDateSelection(date: NgbDate) {
        if (!this.fromDate && !this.toDate) {
            this.fromDate = date;
        } else if (this.fromDate && !this.toDate && date && (date.equals(this.fromDate) || date.after(this.fromDate))) {
            this.toDate = date;
            this.change.emit({startDate: this.fromDate, endDate: this.toDate})
        } else {
            this.toDate = null;
            this.fromDate = date;
        }
    }

    isHovered(date: NgbDate) {
        return this.fromDate && !this.toDate && this.hoveredDate && date.after(this.fromDate) && date.before(this.hoveredDate);
    }

    isInside(date: NgbDate) {
        return this.toDate && date.after(this.fromDate) && date.before(this.toDate);
    }

    isRange(date: NgbDate) {
        return date.equals(this.fromDate) || (this.toDate && date.equals(this.toDate)) || this.isInside(date) || this.isHovered(date);
    }

    validateInput(currentValue: NgbDate | null, input: string): NgbDate | null {
        const parsed = this.formatter.parse(input);
        return parsed && this.calendar.isValid(NgbDate.from(parsed)) ? NgbDate.from(parsed) : currentValue;
    }
}
