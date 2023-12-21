import { Component, Input, ContentChildren, QueryList } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { IconComponent } from '../icon/icon.component';
import { LinkInterface } from '../interfaces/globals.interface';

@Component({
  selector: 'app-link',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './link.component.html',
  styleUrls: ['./link.component.scss']
})
export class LinkComponent {

  @Input() link: LinkInterface = {
    'target': '_self',
    'title': '',
    'url': ''
  };
  @Input() class?: string = '';
  @Input() ariaLabel?: string = '';
  
  // handle external links
  constructor(private location: Location) {}
  
  isExternal = false
  currentHost = window.location.host;

  ngOnInit(): void {
    // see if link passed in is a full url
    if(this.link.url.startsWith('http')) {
      // if yes, compare to this site's url
      this.isExternal = true
      this.link.target = '_blank'

      try {
        const url = new URL(this.link.url, window.location.origin)

        if( url.host !== this.currentHost) {
          this.isExternal = true
          this.link.target = '_blank'
        }
      
      } catch (error) {
        console.error('Invalid URL:', this.link.url)
      }
    }
  }


  // check if there's content, otherwise use the link array
  @ContentChildren('projContent') projectedContent!: QueryList<any>;

  hasContent = false;

  ngAfterContentInit() {
    this.hasContent = this.projectedContent.length > 0
  }
}
