import {Component, Input, OnInit, Output} from '@angular/core';
import {Subject} from "rxjs";
import {isUri} from 'valid-url';

@Component({
  selector: 'app-single-line-text',
  templateUrl: './single-line-text.component.html',
  styleUrls: ['./single-line-text.component.css']
})
export class SingleLineTextComponent implements OnInit {
  @Input() id?: string;
  @Input() parentObject: any;
  @Input() propertyName: string;
  @Input() label: string;
  @Input() placeholder?: string;
  @Input() type: 'text'|'number'|'email' = 'text';
  @Input() requireUrl = false;
  @Input() required = false;
  @Input() subText?: string;
  @Output() change = new Subject<string|number>();
  isUri = isUri;

  constructor() { }

  get isInvalid() {
    if (this.required && !this.parentObject[this.propertyName]) {
      return true;
    }

    if (this.requireUrl) {
      const isValidUrl = isUri(this.parentObject[this.propertyName]);
      if (!isValidUrl) {
        return true;
      }
    }

    return false;
  }

  get valueAsNumber() {
    return this.parentObject[this.propertyName] as number;
  }

  set valueAsNumber(value: number) {
    this.parentObject[this.propertyName] = value;
  }

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.propertyName + "Field";
    }

    if (!this.label) {
      this.label = this.propertyName.substring(0, 1).toUpperCase() + this.propertyName.substring(1);
    }
  }
}
