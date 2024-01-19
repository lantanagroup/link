import { Injectable } from '@angular/core';
import { Tenant, TenantSummary } from 'src/app/shared/interfaces/tenant.model';
import { DataService } from 'src/services/api/data.service';
import { firstValueFrom } from 'rxjs';

interface FacilityQuery {
  page?: number
  sort?: string
  sortAscend?: boolean
  searchCriteria?: string
}

@Injectable({
  providedIn: 'root'
})
export class FacilitiesApiService {

  constructor(private dataService: DataService) { }

  // Fetches all the facilities data
  async fetchAllFacilities(filters: FacilityQuery = {page: 1}): Promise<TenantSummary | null> {
    try {
      const queryParams = new URLSearchParams()
      Object.entries(filters).forEach(([key, value]) => {
        if(value !== undefined && value !== null && value !== '') {
          queryParams.append(key, value)
        }
      })

      const url = queryParams.toString() ? `tenant/summary?${queryParams}` : 'tenant/summary'
      
      const response = await firstValueFrom(this.dataService.getData<any>(url));
      if (response) {
        return response;
      }
      return null
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
