import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

interface ToastData {
  title: string;
  copy: string;
  status: 'success' | 'failed' | 'inProgress';
  show: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastDataSubject = new BehaviorSubject<ToastData>(
    {
      title: '',
      copy: '',
      status: 'inProgress',
      show: false
    }
  )
  toastData$ = this.toastDataSubject.asObservable()

  constructor() { }

  showToast(title: string, copy: string, status: 'success' | 'failed' | 'inProgress') {
    this.toastDataSubject.next({
      title,
      copy,
      status,
      show: true
    })
    // hide notification after a certain period
    // setTimeout(() => this.hideToast(), 5000); 
  }

  hideToast() {
    const currentData = this.toastDataSubject.value
    this.toastDataSubject.next({...currentData, show: false})
  }
}
