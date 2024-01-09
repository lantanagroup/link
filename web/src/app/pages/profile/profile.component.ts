import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MiniContentComponent } from 'src/app/shared/mini-content/mini-content.component';
import { ProfileModel, FjorgeUser } from 'src/app/shared/interfaces/profile.model';
import { ProfileApiService } from 'src/services/api/profile/profile-api.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, ButtonComponent, SectionComponent, CardComponent, MiniContentComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {

  currentUserEmail: string | null = null

  userData: ProfileModel = {
    name: '',
    id: '',
    email: '',
    enabled: true
  }

  constructor(
    private profileApiService: ProfileApiService
  ) {}

  async ngOnInit() {
    // get current email from session storage
    this.currentUserEmail = sessionStorage.getItem('user_email')

    if(this.currentUserEmail) {
      // make api call
      try {
        this.userData = await this.profileApiService.fetchProfileData(this.currentUserEmail)
      } catch (error) {
        console.error('Error loading profile data:', error)
      }
    }
  }
}
