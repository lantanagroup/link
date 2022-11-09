import {Component, Input, OnInit, Output} from '@angular/core';
import {USCoreConfig} from "../../models/config";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';
import {EnvironmentService} from "../../environment.service";
import {Subject} from "rxjs";

@Component({
  selector: 'app-query-uscore',
  templateUrl: './uscore.component.html',
  styleUrls: ['./uscore.component.css']
})
export class UscoreComponent implements OnInit {
  @Input() usCoreConfig: USCoreConfig;
  @Output() change = new Subject();

  faRemove = faRemove;
  faAdd = faAdd;

  constructor(public envService: EnvironmentService) { }

  trackByFn(index: number) {
    return index;
  }

  ngOnInit(): void {
  }

  onChange() {
    this.change.next(null);
  }

}
