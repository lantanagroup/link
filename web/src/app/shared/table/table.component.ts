import { AfterViewInit, Component, Input, OnInit, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTablesModule, DataTableDirective } from 'angular-datatables';
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';
import { TableFilter, SearchBar } from '../interfaces/table.model';
import { debounceTime, distinctUntilChanged, fromEvent, tap } from 'rxjs';

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
  @Input() dtColumns?: [] = [];
  @Input() classes?: string = '';
  @Input() hasSearch?: SearchBar | false = false;
  @Input() filters?: TableFilter[] | null = null;

  // adding custom search
  @ViewChild('customSearch') customSearchInput!: ElementRef
  @ViewChild(DataTableDirective, { static: false })
  datatableElement!: DataTableDirective;


  toggleFilters(): void {
    alert('toggling filters:' + JSON.stringify(this.filters))
  }

  ngAfterViewInit(): void {
    if(this.hasSearch) {
      fromEvent(this.customSearchInput.nativeElement, 'keyup')
        .pipe(
          debounceTime(150),
          distinctUntilChanged(),
          tap(() => {
            this.datatableElement.dtInstance.then((dtInstance: DataTables.Api) => {
              dtInstance.search(this.customSearchInput.nativeElement.value).draw();
            })
          })
        )
        .subscribe()
    }
  }

  ngOnInit(): void {
    if(this.hasSearch) {
      this.dtOptions.searching = true
    }
  }
  constructor() { };
}
