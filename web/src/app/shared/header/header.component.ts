import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../navbar/navbar.component';
import { LinkInterface } from '../interfaces/globals.interface';
import { AuthService } from 'src/services/auth.service';
import { ButtonComponent } from "../button/button.component";

@Component({
    selector: 'app-header',
    standalone: true,
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.scss'],
    imports: [CommonModule, NavbarComponent, ButtonComponent],
    encapsulation: ViewEncapsulation.None,
})
export class HeaderComponent {

  constructor(private authService: AuthService) {}

  // Returns true if user is logged in
  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }
  // Logout the user.
  logout() {
    this.authService.logout();
  }


  secondaryNavItems: LinkInterface[] = [
    { title: 'Profile', url: '/profile'},
    { title: 'System Performance', url: '/system-performance'}
  ];

  primaryNavItems: LinkInterface[] = [
    { title: 'Dashboard', url: '/dashboard'},
    { title: 'Activities', url: '/activities'},
    { title: 'Facilities', url: '/facilities'},
    { title: 'Resources', url: '/resources'}
  ]
}
