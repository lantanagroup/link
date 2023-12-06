import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LinkInterface } from '../interfaces/globals.interface'

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  @Input() navItems: LinkInterface[] = [];
  @Input() navId: string = '';
  @Input() ariaLabel: string = '';
  @Input() navLocation: string = '';
}
