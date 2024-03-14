import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-mini-content',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mini-content.component.html',
  styleUrls: ['./mini-content.component.scss']
})
export class MiniContentComponent {
  @Input() title: string = ''
}
