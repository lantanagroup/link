import {Component, Input, OnInit} from '@angular/core';
import {LinkOAuthConfig} from "../models/config";
import {EnvironmentService} from "../environment.service";
import {isUri} from 'valid-url';

@Component({
  selector: 'app-auth-config',
  templateUrl: './auth-config.component.html',
  styleUrls: ['./auth-config.component.css']
})
export class AuthConfigComponent implements OnInit {
  @Input() parentObject: any;
  @Input() propertyName: string;
  authConfig = new LinkOAuthConfig();
  isUri = isUri;

  constructor(private envService: EnvironmentService) { }

  onChange() {
    // If the  property hasn't been defined on parent config yet because it's new, make sure it gets set here before saving the environment
    this.parentObject[this.propertyName] = this.authConfig;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
    if (this.parentObject[this.propertyName]) {
      this.authConfig = this.parentObject[this.propertyName];
    }
  }
}
