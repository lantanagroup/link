import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResourceSection } from '../models/resource-contents.model';

@Component({
  selector: 'app-resource-contents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './resource-contents.component.html',
  styleUrls: ['./resource-contents.component.scss']
})
export class ResourceContentsComponent {
  @Input() resourceSections: ResourceSection[] = [];
}