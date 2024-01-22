import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { catchError, firstValueFrom, Observable, throwError } from 'rxjs';

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

  getContentObservable<T>(endpoint: string): Observable<T> {
    return this.dataService.getData<T>(endpoint).pipe(
      catchError((error) => {
        console.error('Error fetching data:', error)

        return throwError(() => new Error('Error fetching data from ' + endpoint))
      })
    )
  }
}
