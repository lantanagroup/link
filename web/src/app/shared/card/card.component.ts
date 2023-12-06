import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { ResourceContentsComponent } from '../resource-contents/resource-contents.component';
@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, MetricComponent, ResourceContentsComponent],
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss']
})
export class CardComponent {
  @Input() title: string = "";
  @Input() cardType: string = ""; // Metric, Recent Activity, Resources, etc could be different types of cardTypes

  // Apply specific css based on card type.
  getCardStyles() {
    if (this.cardType === 'metric') {
      return {
        'max-width': '302px',
        'border': '1px solid rgba(255, 255, 255)'
      };
    }
    return {};
  }

  getCardClass() {
    if (this.cardType !== 'metric') {
      return "shadow-sm";
    }
    return "";
  }

  getTitleStyles() {
    if (this.cardType === 'metric') {
      return {
        'background-color': 'rgba(77, 84, 94, 1)'
      };
    }
    return {
      'background-color': 'rgba(2, 27, 58)'
    };
  }

}
