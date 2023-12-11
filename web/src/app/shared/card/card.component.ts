import { Component, Input } from '@angular/core';
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
}
