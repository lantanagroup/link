// login.component.ts

import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  template: `<a [href]="login()">Login</a>`
})
export class LoginComponent {
  constructor(private authService: AuthService) {}

  ngOnInit() {
    console.log('login hit');
  }

  login() {
    // console.log(this.authService.login());

    return this.authService.login();

  }
}
