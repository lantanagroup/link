import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { firstValueFrom } from 'rxjs';
import { TimePeriod } from 'src/app/shared/interfaces/metrics.model';

@Injectable({
  providedIn: 'root'
})
export class MetricApiService {

  constructor(private dataService: DataService) { }

  // Fetches all the facilities data
  async fetchMetrics(period: TimePeriod): Promise<any> {
    try {
      // Fetch the data
      const response = await firstValueFrom(this.dataService.getData<any>(`metric/${period}`));
      if (response) {
        return response;
      }
      return [];
    } catch (error) {
      console.error('Error fetching last weeks metrics:', error);
      throw error;
    }
  }
  // ... other methods http methods coming soon ...
}
