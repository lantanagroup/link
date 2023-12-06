import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardComponent } from 'src/app/shared/card/card.component';
import { ResourceContentsComponent } from 'src/app/shared/resource-contents/resource-contents.component';
import { ResourceGroup } from 'src/app/shared/models/resource-contents.model';
import { HeroComponent } from 'src/app/shared/hero/hero.component';

@Component({
  selector: 'app-resources',
  standalone: true,
  imports: [CommonModule, HeroComponent, CardComponent, ResourceContentsComponent],
  templateUrl: './resources.component.html',
  styleUrls: ['./resources.component.scss']
})
export class ResourcesComponent {

  //Test data
  resourceGroups: ResourceGroup[] = [
    {
      name: 'DQM: INFO, SPECS AND HISTORY',
      sections: [
        {
          name: 'Instruction Book',
          links: [
            { title: 'instruction book', url: 'https://example.com/1.1' },
          ]
        },
        {
          name: 'Measures',
          links: [
            { title: 'Hospital-associated venous thromboembolism (VTE)', url: 'https://example.com/2.1', target: '_blank' },
            { title: 'Medication-related hypoglycemia (HYPO)', url: 'https://example.com/2.2', target: '_blank' },
            { title: 'Inpatient hyperglycemia (HYPR)', url: 'https://example.com/2.2', target: '_blank' },
            { title: 'C. difficile infection (CDI)', url: 'https://example.com/2.2', target: '_blank' },
            { title: 'Hospital-onset bacteremia (HOB)', url: 'https://example.com/2.2', target: '_blank' },
            { title: 'Respiratory pathogen surveillance (RPS)', url: 'https://example.com/2.2', target: '_blank' }
          ]
        }
      ]
    },
    {
      name: 'ECQI: TOOLS AND RESOURCE KEY',
      sections: [
        {
          name: 'Tools and Resources',
          links: [
            { title: 'eCQI Tools and Resources Library', url: 'https://example.com/3.1', target: '_blank' },
            { title: 'ECQM Standards and Tools Variations', url: 'https://example.com/3.2', target: '_blank' }
          ]
        }
      ]
    },
    {
      name: 'Fjorge CONFLUENCE URLS',
      sections: [
        {
          name: 'Confluence URLS',
          links: [
            { title: 'confluence_URL 1', url: 'https://example.com/3.1', target: '_blank' },
            { title: 'confluence_URL 2', url: 'https://example.com/3.2', target: '_blank' },
            { title: 'confluence_URL 3', url: 'https://example.com/3.1', target: '_blank' },
            { title: 'confluence_URL 4', url: 'https://example.com/3.2', target: '_blank' }
          ]
        }
      ]
    }
  ];


}
