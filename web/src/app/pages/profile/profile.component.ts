import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { jwtDecode } from 'jwt-decode';
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
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent {

  accessToken: string | null = null
  apiUserData: ProfileModel | null = null

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
    this.accessToken = sessionStorage.getItem('access_token')

    if(this.accessToken) {
      // Decode the access token
      const decodedToken: any = jwtDecode(this.accessToken)
  
      // Extract the email and save it to the session storage
      this.userData.email = decodedToken?.email
      this.userData.name = decodedToken?.name
      
      try {
        this.apiUserData = await this.profileApiService.fetchProfileData(this.userData?.email)
        this.userData.id = this.apiUserData.id
      } catch (error) {
        console.error('Error fetching API user data:', error)
      }
    }
  }
}
