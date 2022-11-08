import {Component, Input, OnInit} from '@angular/core';
import {ApiConfig, ApiCorsConfig} from "../../models/config";
import {EnvironmentService} from "../../environment.service";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-api-cors',
  templateUrl: './cors.component.html',
  styleUrls: ['./cors.component.css']
})
export class CorsComponent implements OnInit {
  @Input() apiConfig: ApiConfig;
  cors = new ApiCorsConfig();
  faRemove = faRemove;
  faAdd = faAdd;

  constructor(public envService: EnvironmentService) { }

  trackByIndex(index: number) {
    return index;
  }

  onChange() {
    // If the cors property hasn't been defined on apiConfig yet because it's new, make sure it gets set here before saving the environment
    this.apiConfig.cors = this.cors;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
    if (this.apiConfig.cors) {
      this.cors = this.apiConfig.cors;
    }
  }
}
