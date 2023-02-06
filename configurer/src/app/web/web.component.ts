import {Component, OnInit} from '@angular/core';
import {EnvironmentService} from "../environment.service";

@Component({
  selector: 'app-web',
  templateUrl: './web.component.html',
  styleUrls: ['./web.component.css']
})
export class WebComponent implements OnInit {
  // webConfig: WebConfig;

  constructor(public envService: EnvironmentService) {
  }

  // get oauthIssuer() {
  //   if (!this.webConfig.oauth) {
  //     return '';
  //   }
  //   return this.webConfig.oauth.issuer;
  // }
  //
  // set oauthIssuer(value: string) {
  //   if (!this.webConfig.oauth) {
  //     this.webConfig.oauth = new WebOAuthConfig();
  //   }
  //   this.webConfig.oauth.issuer = value;
  // }
  //
  // get oauthClientId() {
  //   if (!this.webConfig.oauth) {
  //     return '';
  //   }
  //   return this.webConfig.oauth.clientId;
  // }
  //
  // set oauthClientId(value: string) {
  //   if (!this.webConfig.oauth) {
  //     this.webConfig.oauth = new WebOAuthConfig();
  //   }
  //   this.webConfig.oauth.clientId = value;
  // }
  //
  // get oauthScope() {
  //   if (!this.webConfig.oauth) {
  //     return '';
  //   }
  //   return this.webConfig.oauth.scope;
  // }
  //
  // set oauthScope(value: string) {
  //   if (!this.webConfig.oauth) {
  //     this.webConfig.oauth = new WebOAuthConfig();
  //   }
  //   this.webConfig.oauth.scope = value;
  // }

  ngOnInit(): void {
    // this.webConfig = this.envService.webConfig;
  }
}
