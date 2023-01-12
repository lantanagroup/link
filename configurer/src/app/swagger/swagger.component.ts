import {Component, Input, OnInit} from '@angular/core';
import {ApiConfigWrapper} from "../models/config-wrappers";
import {SwaggerConfig} from "../models/config";
import {EnvironmentService} from "../environment.service";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-swagger',
  templateUrl: './swagger.component.html',
  styleUrls: ['./swagger.component.css']
})
export class SwaggerComponent implements OnInit {
  @Input() apiConfig: ApiConfigWrapper;
  swagger = new SwaggerConfig();
  faRemove = faRemove;
  faAdd = faAdd;

  constructor(public envService: EnvironmentService) {
  }

  trackByIndex(index: number) {
    return index;
  }

  onChange() {
    // If the cors property hasn't been defined on apiConfig yet because it's new, make sure it gets set here before saving the environment
    this.apiConfig.swagger = this.swagger;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
    if (this.apiConfig.swagger) {
      this.swagger = this.apiConfig.swagger;
    }
  }

}

