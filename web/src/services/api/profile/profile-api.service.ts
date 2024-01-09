import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { ProfileModel } from 'src/app/shared/interfaces/profile.model';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProfileApiService {

  constructor(private dataService: DataService) { }

  async fetchProfileData(userEmail: string): Promise<ProfileModel> {
    try {
      const response = await firstValueFrom(this.dataService.getData<any>(`user?email=${userEmail}`))
      console.log('response:', response)
      return response[0]
    } catch (error) {
      console.error('Error fetching user data', error)
      throw error
    }
  }
}
