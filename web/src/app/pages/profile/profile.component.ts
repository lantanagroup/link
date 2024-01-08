import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MiniContentComponent } from 'src/app/shared/mini-content/mini-content.component';
import { UserModel, FjorgeUser } from 'src/app/shared/interfaces/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, ButtonComponent, SectionComponent, CardComponent, MiniContentComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {

  userData: UserModel = {
    name: '',
    userId: '',
    email: ''
  }

  ngOnInit() {
    this.userData = FjorgeUser
  }
}
