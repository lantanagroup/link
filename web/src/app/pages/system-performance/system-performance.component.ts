import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { CardComponent } from 'src/app/shared/card/card.component';

@Component({
  selector: 'app-system-performance',
  standalone: true,
  imports: [CommonModule, HeroComponent, ButtonComponent, IconComponent, CardComponent],
  templateUrl: './system-performance.component.html',
  styleUrls: ['./system-performance.component.css']
})
export class SystemPerformanceComponent {

}
