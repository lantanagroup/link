// ****************************************************************************
// ! This is here potentially for V2, currently this is handled within Keycloak
// ****************************************************************************

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AbstractControl, FormBuilder, FormControl, FormGroup, Validators, ValidationErrors } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { FjorgeUser, ProfileModel } from 'src/app/shared/interfaces/profile.model';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ToastComponent } from 'src/app/shared/toast/toast.component';
import { ToastService } from 'src/services/toasts/toast.service';

@Component({
  selector: 'app-update-password',
  standalone: true,
  imports: [CommonModule, ButtonComponent, IconComponent, SectionComponent, CardComponent, HeroComponent, ToastComponent, ReactiveFormsModule],
  templateUrl: './update-password.component.html',
  styleUrls: ['./update-password.component.scss']
})
export class UpdatePasswordComponent {

  userData: ProfileModel = {
    name: '',
    id: '',
    email: '',
    enabled: true
  }

  ngOnInit() {
    this.userData = FjorgeUser
  }

  // update password form
  constructor(private fb: FormBuilder, private toastService: ToastService, private router: Router) {}

  updatePasswordForm = new FormGroup({
    newPassword: new FormControl('', [Validators.required]),
    confirmPassword: new FormControl('', [Validators.required])
  })

  checkPasswords(group: FormGroup): ValidationErrors | null {
    const pass = group.get('newPassword')?.value,
          confirmPass = group.get('confirmPassword')?.value
          
    return pass === confirmPass ? null : { matchError: 'New passwords do not match' };
  }

  onSubmit() {
    const passwordValidation = this.checkPasswords(this.updatePasswordForm)

    if(passwordValidation) {
      this.updatePasswordForm.setErrors(passwordValidation)
    }

    if (this.updatePasswordForm.valid) {
      // todo : hit API
      this.toastService.showToast(
        'Password Updated',
        `Password for user ${this.userData.name} has been successfully updated.`,
        'success'
      )
      this.router.navigate(['/profile/'])

      // todo : fail
      // this.toastService.showToast(
      //   'Error',
      //   `Password has not been updated due to an error, please try again.`,
      //   'failed'
      // )
    }
  }
  
}
