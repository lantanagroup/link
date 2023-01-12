import {Injectable} from '@angular/core';

export interface ToastInfo {
  header: string;
  body: string;
  delay?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts: ToastInfo[] = [];

  show(header: string, body: string, delay?: number) {
    const newToast = { header, body, delay };
    this.toasts.push(newToast);
  }

  remove(toast: ToastInfo) {
    this.toasts = this.toasts.filter(t => t != toast);
  }
}
