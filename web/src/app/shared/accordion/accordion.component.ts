import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-accordion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accordion.component.html',
  styleUrls: ['./accordion.component.css']
})
export class AccordionComponent {
  @Input() title: string = "";
  @Input() isExpanded: boolean = false;

  // Format the title to generate unique ids. This will be used to tigger accordion open and collapse.
  formatTitle(title: string): string {
    // Remove spaces and special characters from the title to create a valid ID
    return title.replace(/\s+/g, '-').replace(/[^a-zA-Z0-9-]/g, '').toLowerCase();
  }
}
