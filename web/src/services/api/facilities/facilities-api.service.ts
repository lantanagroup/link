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

  // Fetches all the facilities data
  async fetchFacilityById(id: string): Promise<any> {
    try {
      const response = await firstValueFrom(this.dataService.getData<any>(`tenant/${id}`));
      if (response) {
        console.log(response, 'tenant by id');
        return response;
      }
      return [];
    } catch (error) {
      console.error('Error fetching tenant summary data', error);
      throw error;
    }
  }

  // Adds a new facility
  async createFacility(facilityData: any): Promise<any> {
    try {
      const response = await firstValueFrom(this.dataService.postData<any>('tenant', facilityData));
      // console.log(response, 'new facility created');
      return response;
    } catch (error) {
      console.error('Error creating new facility', error);
      throw error;
    }
  }

  // Updates an existing facility by ID
  async updateFacility(id: string, facilityData: any): Promise<any> {
    try {
      debugger;
      const response = await firstValueFrom(this.dataService.putData<any>(`tenant/${id}`, facilityData));
      // console.log(response, 'facility updated');
      return response;
    } catch (error) {
      console.error('Error updating facility', error);
      throw error;
    }
  }


  // ... other methods http methods coming soon ...
}
