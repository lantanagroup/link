import {Component, OnInit} from '@angular/core';
import {EnvironmentService} from "./environment.service";
import {Router} from "@angular/router";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {faAdd, faEdit, faRemove} from '@fortawesome/free-solid-svg-icons';
import {ConfigTypes} from "./models/config";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  faAdd = faAdd;
  faEdit = faEdit;
  faRemove = faRemove;

  constructor(private modal: NgbModal, public envService: EnvironmentService, public router: Router) {
  }


   remove(configType: ConfigTypes) {
    switch (configType) {
      case 'api':
        this.envService.apiConfig = null;
         break;
      case 'web':
        this.envService.webConfig = null;
         break;
      case 'consumer':
       this.envService.consumerConfig = null;
         break;
    }
    this.envService.removeConfig(configType);
    this.router.navigate(['/']);
  }

  async ngOnInit() {

  }
}
