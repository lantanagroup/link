import {Component, Input, OnInit} from '@angular/core';
import {EnvironmentService} from '../environment.service';
import {DataStore} from "../models/config";

@Component({
  selector: 'app-datastore',
  templateUrl: './datastore.component.html',
  styleUrls: ['./datastore.component.css']
})

export class DatastoreComponent implements OnInit {
  @Input() parentObject: any;
  @Input() propertyName: string;
  dataStore = new DataStore();

  constructor(private envService: EnvironmentService) { }

  onChange() {
    // If the  property hasn't been defined on parent config yet because it's new, make sure it gets set here before saving the environment
    this.parentObject[this.propertyName] = this.dataStore;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
    if (this.parentObject[this.propertyName]) {
      this.dataStore = this.parentObject[this.propertyName];
    }
  }
}
