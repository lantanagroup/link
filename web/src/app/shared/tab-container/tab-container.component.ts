import { AfterContentInit, Component, ContentChildren, QueryList, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabComponent } from '../tab/tab.component';
import { IdFromTitlePipe } from 'src/app/helpers/GlobalPipes.pipe';

@Component({
  selector: 'app-tab-container',
  standalone: true,
  imports: [CommonModule, IdFromTitlePipe],
  templateUrl: './tab-container.component.html',
  styleUrls: ['./tab-container.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TabContainerComponent implements AfterContentInit {
  @ContentChildren(TabComponent) tabs!: QueryList<TabComponent>;

  ngAfterContentInit() {
    const activeTabs = this.tabs.filter(tab => tab.isActive);
    if (activeTabs.length === 0) {
      this.selectTab(this.tabs.first);
    }
  }

  selectTab(tab: TabComponent) {
    this.tabs.toArray().forEach(t => t.isActive = false);
    tab.isActive = true;
  }
}
