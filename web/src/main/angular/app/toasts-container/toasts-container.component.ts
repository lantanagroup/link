import {Component} from '@angular/core';
import {ToastService} from '../toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <ngb-toast
      *ngFor="let toast of toastService.toasts"
      [header]="toast.header || 'Info'"
      [class]="toast.className"
      [autohide]="!!toast.delay"
      [delay]="toast.delay"
      (hide)="toastService.remove(toast)">
      <ng-template [ngIf]="toast.isTemplate" [ngIfElse]="text">
        <ng-template [ngTemplateOutlet]="toast.textOrTpl"></ng-template>
      </ng-template>

      <ng-template #text>{{ toast.textOrTpl }}</ng-template>
    </ngb-toast>
  `,
  host: {'[class.ngb-toasts]': 'true'}
})
export class ToastsContainerComponent {
  constructor(public toastService: ToastService) {}
}