import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginComponent } from 'src/app/login/login.component';
import { NavbarComponent } from '../navbar/navbar.component';
import { LinkInterface } from '../interfaces/globals.interface';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  
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
