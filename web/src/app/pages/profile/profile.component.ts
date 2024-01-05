import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MiniContentComponent } from 'src/app/shared/mini-content/mini-content.component';

interface userData {
  name: string
  userId: string
  email: string
  role?: string
  organization?: string
  department?: string
  phone?: string
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, ButtonComponent, SectionComponent, CardComponent, MiniContentComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {

  userData: userData = {
    name: '',
    userId: '',
    email: ''
  }

  ngOnInit() {
    this.userData = {
      name: 'Fjorge Developers',
      userId: 'ef0a782d-1e0f-4846-9c67-24f63d855a7e',
      email: 'developers@fjorgedigital.com',
      role: 'to come',
      organization: 'fjorge',
      department: 'tech',
      phone: '123-456-7890'
    }
  }
}
