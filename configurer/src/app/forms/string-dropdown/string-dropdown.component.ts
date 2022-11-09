import {Component, Input, OnInit, Output} from '@angular/core';
import {Subject} from "rxjs";

@Component({
  selector: 'app-string-dropdown',
  templateUrl: './string-dropdown.component.html',
  styleUrls: ['./string-dropdown.component.css']
})
export class StringDropdownComponent implements OnInit {
  @Input() id?: string;
  @Input() label: string;
  @Input() parentObject: any;
  @Input() propertyName: string;
  @Input() required = false;
  @Input() options: string[];
  @Input() selectLabel = 'SELECT';
  @Output() change = new Subject<void>();

  constructor() { }

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.propertyName + 'Field';
    }

    if (!this.parentObject[this.propertyName] && this.required && this.options && this.options.length > 0) {
      this.parentObject[this.propertyName] = this.options[0];
      this.change.next(null);
    }
  }

}
