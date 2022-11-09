import {Component, Input, OnInit, Output} from '@angular/core';
import {Subject} from "rxjs";

@Component({
  selector: 'app-boolean',
  templateUrl: './boolean.component.html',
  styleUrls: ['./boolean.component.css']
})
export class BooleanComponent implements OnInit {
  @Input() id: string;
  @Input() parentObject: any;
  @Input() propertyName: string;
  @Input() label: string;
  @Input() defaultValue?: boolean;
  @Input() subText?: string;
  @Output() change = new Subject<string>();

  constructor() { }

  ngOnInit(): void {
    if (!this.parentObject.hasOwnProperty(this.propertyName) && (this.defaultValue === true || this.defaultValue === false)) {
      this.parentObject[this.propertyName] = this.defaultValue;
      this.change.next(null);
    }
  }

}
