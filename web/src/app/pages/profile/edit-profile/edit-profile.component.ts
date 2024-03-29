// ****************************************************************************
// ! This is here potentially for V2, currently this is handled within Keycloak
// ****************************************************************************

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators, ValidationErrors, EmailValidator } from '@angular/forms'
import { ReactiveFormsModule } from '@angular/forms';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ToastComponent } from 'src/app/shared/toast/toast.component';
import { ToastService } from 'src/services/toasts/toast.service';
import { ProfileModel } from 'src/app/shared/interfaces/profile.model';
import { ProfileApiService } from 'src/services/api/profile/profile-api.service';

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent, SectionComponent, CardComponent, HeroComponent, IconComponent, ToastComponent],
  templateUrl: './edit-profile.component.html',
  styleUrls: ['./edit-profile.component.scss']
})
export class EditProfileComponent {

  currentUserEmail: string | null = null
  userData: ProfileModel = {
    name: '',
    id: '',
    email: '',
    enabled: true
  }

  async ngOnInit() {
    // get current email from session storage
    this.currentUserEmail = sessionStorage.getItem('user_email')

    if(this.currentUserEmail) {
      // make api call
      try {
        this.userData = await this.profileApiService.fetchProfileData(this.currentUserEmail)

        this.setInitialValues()
      } catch (error) {
        console.error('Error loading profile data:', error)
      }
    }
  }

  constructor(
    private profileApiService: ProfileApiService,
    private fb: FormBuilder,
    private toastService: ToastService, 
    private router: Router
  ) {}

  updateProfileForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required]),
    role: new FormControl(''),
    organization: new FormControl(''),
    department: new FormControl(''),
    phone: new FormControl('')
  })

  private setInitialValues(): void {
    this.updateProfileForm.patchValue({
      name: this.userData?.name,
      email: this.userData?.email,
      role: this.userData?.role,
      organization: this.userData?.organization,
      department: this.userData?.department,
      phone: this.userData?.phone
    })
  }

  onSubmit() {
    if (this.updateProfileForm.valid) {
      // todo : hit API
      this.toastService.showToast(
        'Profile Updated',
        `Profile for user ${this.userData.name} has been successfully updated.`,
        'success'
      )
      this.router.navigate(['/profile/'])

      // todo : fail
      // this.toastService.showToast(
      //   'Error',
      //   `Profile has not been updated due to an error, please try again.`,
      //   'failed'
      // )
    }
  }
}
