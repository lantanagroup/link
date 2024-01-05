import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { firstValueFrom } from 'rxjs';
import { ReportFilter } from 'src/app/shared/interfaces/report.model';

@Injectable({
  providedIn: 'root'
})
export class ReportApiService {

  constructor(private dataService: DataService) { }

  // Fetches all the facilities data
  async fetchAllReport(filters: ReportFilter = {}): Promise<any> {
    try {
      // Building the query string from the filters object
      const queryParams = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          queryParams.append(key, value.toString());
        }
      });

      // Determine the URL to call based on whether there are filters
      const url = queryParams.toString() ? `report/?${queryParams}` : 'report/';

      // Fetch the data
      const response = await firstValueFrom(this.dataService.getData<any>(url));
      if (response) {
        return response;
      }
      return [];
    } catch (error) {
      console.error('Error fetching report', error);
      throw error;
    }
  }
  // ... other methods http methods coming soon ...
}
