import {Injectable, TemplateRef} from '@angular/core';

export class ToastOptions {
  className?: string;
  delay?: number;
  header? = 'Info';
}

export class ToastInstance extends ToastOptions {
  textOrTpl?: any;

  get isTemplate() {
    return this.textOrTpl instanceof TemplateRef;
  }
}

@Injectable()
export class ToastService {
  toasts: ToastInstance[] = [];

  show(textOrTpl: string | TemplateRef<any>, options: ToastOptions = {}) {
    const instance = new ToastInstance();
    instance.textOrTpl = textOrTpl;
    Object.assign(instance, options);
    this.toasts.push(instance);
  }

  showInfo(message: string) {
    this.show(message, { delay: 8000 });
  }

  showError(message: string) {
    this.show(message, { className: 'bg-danger text-light', header: 'Error' });
  }

  showException(header: string, ex: any) {
    let message = 'Unknown error';

    if (ex) {
      if (ex.error && ex.error.message) {
        message = ex.error.message;
      } else if (ex.message) {
        message = ex.message;
      }
    }

    this.show(message, { className: 'bg-danger text-light', header });
  }

  remove(toast) {
    this.toasts = this.toasts.filter(t => t !== toast);
  }
}
