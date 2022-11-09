import {Component, Input, OnInit} from '@angular/core';
import {EnvironmentService} from 'src/app/environment.service';
import {ApiConfig, ApiConfigEvents} from 'src/app/models/config';
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-event',
  templateUrl: './event.component.html',
  styleUrls: ['./event.component.css']
})

export class EventComponent implements OnInit {
  @Input()  apiConfig: ApiConfig;
  @Input()  eventName: any;

  events = new ApiConfigEvents();
  classes : string[];
  faRemove = faRemove;
  faAdd = faAdd;


  constructor(public envService: EnvironmentService) { }

  trackByIndex(index: number) {
    return index;
  }

  onChange() {
    // If the cors property hasn't been defined on apiConfig yet because it's new, make sure it gets set here before saving the environment
    this.apiConfig.events[this.eventName as keyof ApiConfigEvents] = this.classes;
    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
    if (this.apiConfig.events) {
      this.events = this.apiConfig.events;
      this.classes = this.apiConfig.events[this.eventName as keyof ApiConfigEvents];
    }
  }
}
