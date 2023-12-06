import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, ButtonComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {

}
