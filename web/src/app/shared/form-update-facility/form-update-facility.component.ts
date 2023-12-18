import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { AccordionComponent } from '../accordion/accordion.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-form-update-facility',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SectionHeadingComponent, AccordionComponent, ButtonComponent, IconComponent],
  templateUrl: './form-update-facility.component.html',
  styleUrls: ['./form-update-facility.component.scss']
})
export class FormUpdateFacilityComponent {

  facilitiesForm = new FormGroup({
    profile: new FormGroup({
      tenantId: new FormControl('', [Validators.required]),
      name: new FormControl('', [Validators.required]),
      description: new FormControl(''),
      bundleName: new FormControl('', [Validators.required]),
      cdcOrgId: new FormControl(''),
      database: new FormControl(''),
      vendor: new FormControl(''),
      dataRetentionPeriod: new FormControl('')
    }),
    normalizations: new FormGroup({
      codeSystemCleanup: new FormControl(0),
      containedResourceCleanup: new FormControl(0),
      copyLocation: new FormControl(0),
      encounterStatus: new FormControl(0),
      fixPeriodDates: new FormControl(0),
      fixResourceIds: new FormControl(0),
      patientDataResource: new FormControl(0)
    })
  });

  onSubmit() {
    if (this.facilitiesForm.valid) {
      console.log(this.facilitiesForm.value)
    } else {
      this.facilitiesForm.markAllAsTouched()
    }
  }
}
