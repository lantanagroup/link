import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataService } from 'src/services/data.service';

@Component({
  selector: 'app-tenant-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tenant-reports.component.html',
  styleUrls: ['./tenant-reports.component.css']
})
export class TenantReportsComponent {
  constructor(private dataService: DataService) { }


  loadData() {
    this.dataService.getProtectedData().subscribe(data => {
      console.log(data);
      // Handle the data
    });
  }

  // ngOnInit() {
  //   this.dataService.getProtectedData().subscribe(data => {
  //     console.log(data);
  //     // Handle the data
  //   })
  // };


}
