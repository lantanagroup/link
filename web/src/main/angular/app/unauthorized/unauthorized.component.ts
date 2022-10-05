import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Data} from '@angular/router';
import {AuthService} from "../services/auth.service";

@Component({
  selector: 'app-unauthorized-page',
  templateUrl: './unauthorized.component.html',
  styleUrls: ['./unauthorized.component.css']
})
export class UnauthorizedComponent implements OnInit {
  errorMessage: string;
  tokenExpired: boolean = false;

  constructor(private route: ActivatedRoute, private authService: AuthService) {
  }

  ngOnInit() {
    this.route.data.subscribe(
        (data: Data) => {
          this.errorMessage = data['message'];
        }
    );

    //check auth status
    let tokenExperiation = this.authService.oauthService.getAccessTokenExpiration();
    this.tokenExpired = Date.now() > tokenExperiation;

  }

  public user() {
    return this.authService.user;
  }

  login() {
    this.authService.loginLocal();
  }

  logout() {
    this.authService.logout();
  }





}
