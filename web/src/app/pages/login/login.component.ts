import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { AuthService } from 'src/services/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ButtonComponent, IconComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  public message: string | null = null
  public errorMessage: string | null = null

  constructor(
    private authService: AuthService, 
    private router: Router, 
  ) { }

  // If user is already logged in, redirect to dashboard.
  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']); // Redirect to dashboard if logged in
    }

    // show messages
    this.message = sessionStorage.getItem('loginMessage')
    if(this.message) {
      this.message = decodeURIComponent(this.message)
    }
    sessionStorage.removeItem('loginMessage')
  }
  // Returns the sign-in redirect url to keycloak.
  signIn(): string {
    return this.authService.login();
  }
}
