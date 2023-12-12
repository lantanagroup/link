import { AfterViewInit, Component, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTablesModule } from 'angular-datatables';
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [CommonModule, DataTablesModule, ButtonComponent, IconComponent, SectionHeadingComponent],
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TableComponent implements OnInit {
  @Input() tableTitle: string = '';
  @Input() dtOptions: DataTables.Settings = {};

  ngOnInit(): void {

  }
  constructor() { };
}
