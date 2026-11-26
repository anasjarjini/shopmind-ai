import { Component, inject } from '@angular/core';
import { NotificationService, Toast } from '../../../core/services/notification.service';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.html'
})
export class ToastComponent {
  readonly notification = inject(NotificationService);
  readonly toasts       = this.notification.toasts;

  dismiss(id: number): void {
    this.notification.remove(id);
  }

  trackById(_: number, toast: Toast): number {
    return toast.id;
  }
}
