import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { AppData } from 'src/app/shared/interfaces/app.model';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AppApiService {

  constructor(private dataService: DataService) { }

  async fetchAppData(): Promise<AppData> {
    try {
      const response = await firstValueFrom(this.dataService.getData<any>(''))
      console.log('apiData', response)
      return response
    } catch (error) {
      console.error('Error fetching API data', error)
      throw error
    }
  }
}
