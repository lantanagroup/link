import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from 'src/app/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent implements OnInit {
  @Input() title: string = '';
  @Input() copy: string = '';
  @Input() status: 'success' | 'failed' | 'inProgress' = 'inProgress';
  @Input() show: boolean = false;

  constructor(private toastService: ToastService) {}

  handleClose(): void {
    this.toastService.hideToast()
  }

  ngOnInit(): void {
    this.toastService.toastData$.subscribe(data => {
      this.title = data.title;
      this.copy = data.copy;
      this.status = data.status;
      this.show = data.show;
    })
  }
}
