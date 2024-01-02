import { Component, Input, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss']
})
export class CardComponent {
  @Input() title: string = "";
  @Input() variant?: 'standard' | 'alt' = 'standard';
  @Input() titleAlign?: 'left' | 'right' | 'center' = 'left';
  @Input() status?: 'success' | 'failure' | 'inProgress' | null = null;
  @Input() hasPadding?: boolean = false;
  @Input() hasBorder?: boolean = false;
  
  
  getCardClass = () => {
    const classes = ['card']

    if (this.variant) {
      classes.push('card--' + this.variant)
    }

    if (this.hasBorder) {
      classes.push('card--border')
    }

    return classes.join(' ')
  }

}
