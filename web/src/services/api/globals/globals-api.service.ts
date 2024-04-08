import { Injectable } from '@angular/core';
import { DataService } from 'src/services/api/data.service';
import { catchError, firstValueFrom, Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GlobalApiService {
  constructor(private dataService: DataService) {}

  // GET
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

  getContentObservable<T>(path: string): Observable<T> {
    return this.dataService.getData<T>(path).pipe(
      catchError((error) => {
        console.error('Error fetching data:', error)

        return throwError(() => new Error('Error fetching data from ' + path))
      })
    )
  }

  // POST
  async postContent<T>(path: string, data: any): Promise<any> {
    try {
      const response = await firstValueFrom(this.dataService.postData<any>(path, data))
      if (response) {
        return response
      }
      return []
    } catch (error) {
      console.error('Error posting data:', error)
      throw error
    }
  }

  postContentObservable<T>(path: string, data: any): Observable<T> {
    return this.dataService.postData<T>(path, data).pipe(
      catchError((error) => {
        throw error
      })
    )
  }

  // PUT
  async putContent<T>(path: string, data: any): Promise<any> {
    try {
      const response = await firstValueFrom(this.dataService.putData<any>(path, data))
      if (response) {
        return response
      }
      return []
    } catch (error) {
      console.error('Error putting data:', error)
      throw error
    }
  }

  putContentObservable<T>(path: string, data: any): Observable<T> {
    return this.dataService.putData<T>(path, data).pipe(
      catchError((error) => {
        throw error
      })
    )
  }
}
