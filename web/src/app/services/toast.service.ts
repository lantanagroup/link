import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ToastData } from '../shared/interfaces/toast.model';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastData[]>([])
  toasts$ = this.toastsSubject.asObservable()

  constructor() { }

  showToast(title: string, copy: string, status: 'success' | 'failed' | 'inProgress') {
    const newToast: ToastData = {
      title,
      copy,
      status
    } 
    this.toastsSubject.next([newToast, ...this.toastsSubject.value])
    // hide notification after a certain period
    // setTimeout(() => this.hideToast(), 5000); 
  }

  removeToast(index: number) {
    const toasts = this.toastsSubject.value.filter((_, i) => i !== index)
    this.toastsSubject.next(toasts)
  }
}
