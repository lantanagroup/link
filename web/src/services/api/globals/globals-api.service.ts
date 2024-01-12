import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GlobalApiService {
  constructor(private dataService: DataService) {}

  async getContent<T>(path: string): Promise<any> {
    try {
      const response = await firstValueFrom(this.dataService.getData<T>(path))
      if (response) {
        return response
      }
      return []
    } catch (error) {
      console.error('Error fetching data:', error)
      throw error
    }
  }
}
