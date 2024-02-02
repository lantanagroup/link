import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IdFromTitlePipe } from 'src/app/helpers/GlobalPipes.pipe';

@Component({
  selector: 'app-tab',
  standalone: true,
  imports: [CommonModule, IdFromTitlePipe],
  templateUrl: './tab.component.html',
  styleUrls: ['./tab.component.scss']
})
export class TabComponent {
  @Input() tabTitle: string = ''
  @Input() isActive: boolean = false
}
