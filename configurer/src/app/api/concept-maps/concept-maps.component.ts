import {Component, Input, OnInit} from '@angular/core';
import {EnvironmentService} from "../../environment.service";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';
import {ConceptMapConfig} from 'src/app/models/config';
import {ApiConfigWrapper} from 'src/app/models/config-wrappers';

@Component({
  selector: 'app-api-concept-maps',
  templateUrl: './concept-maps.component.html',
  styleUrls: ['./concept-maps.component.css']
})
export class ConceptMapsComponent implements OnInit {
  @Input() apiConfig: ApiConfigWrapper;
  faRemove = faRemove;
  faAdd = faAdd;
  conceptMaps: ConceptMapConfig[] = [];

  constructor(public envService: EnvironmentService) { }

  onChange() {
    // If the cors property hasn't been defined on apiConfig yet because it's new, make sure it gets set here before saving the environment
    this.apiConfig.applyConceptMaps.conceptMaps = this.conceptMaps;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  onClick(){
    let fhirContextPath: string[] = [];
    let conceptMapConfig = {"conceptMapId": '', "fhirPathContexts": fhirContextPath}
    this.apiConfig.applyConceptMaps.conceptMaps.push(conceptMapConfig);
  }


  trackByMethod(index:number, el:any): number {
    return index;
  }

  ngOnInit(): void {
    this.apiConfig.applyConceptMaps.conceptMaps = this.apiConfig.applyConceptMaps.conceptMaps || [];
    this.conceptMaps = this.apiConfig.applyConceptMaps.conceptMaps;
  }

}
