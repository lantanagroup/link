import { Component, OnInit } from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {OAuthService} from 'angular-oauth2-oidc';
import {AuthInitOptions, AuthService} from '../auth.service';

@Component({
  selector: 'app-smart-login',
  templateUrl: './smart-login.component.html',
  styleUrls: ['./smart-login.component.css']
})
export class SmartLoginComponent implements OnInit {
  public message: string;

  constructor(private route: ActivatedRoute, private router: Router, private http: HttpClient, private authService: AuthService) {
  }

  async ngOnInit() {
    this.route.queryParams.subscribe(async (params) => {
      if (params.error) {
        this.message = params.error;
      } else if (params.state) {
        await this.authService.loginSmart(params.state);
      } else if (params.launch && params.iss) {
        await this.authService.launchSmart(params.launch, params.iss);
      } else {
        this.message = 'Not sure how to proceed...';
      }
    });
  }
}
