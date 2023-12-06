import { AfterViewInit, Component, Input, OnInit } from '@angular/core';
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
  styleUrls: ['./table.component.scss']
})
export class TableComponent implements OnInit {
  @Input() tableTitle: string = '';
  @Input() dtOptions: DataTables.Settings = {};

  ngOnInit(): void {

  }

  // This method will be replaced by something else
  showAlert(): void {
    alert('Filters coming out soon.');
  }

  constructor() { };
}
