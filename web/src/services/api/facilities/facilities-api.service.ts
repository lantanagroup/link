import { Injectable } from '@angular/core';
import { Tenant, TenantSummary } from 'src/app/shared/interfaces/tenant.model';
import { DataService } from 'src/services/api/data.service';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FacilitiesApiService {

  constructor(private dataService: DataService) { }

  // Fetches all the facilities data
  async fetchAllFacilities(): Promise<Tenant[]> {
    try {
      const response = await firstValueFrom(this.dataService.getData<TenantSummary>('tenant/summary'));
      if (response) {
        return response.tenants;
      }
      return [];
    } catch (error) {
      console.error('Error fetching tenant summary data', error);
      throw error;
    }
  }

  // Fetches all single facility data
  async fetchFacilityById(id: string): Promise<any> {
    try {
      const response = await firstValueFrom(this.dataService.getData<any>(`tenant/${id}`));
      if (response) {
        return response;
      }
      return [];
    } catch (error) {
      console.error('Error fetching tenant summary data', error);
      throw error;
    }
  }

  // ... other methods http methods coming soon ...
}
