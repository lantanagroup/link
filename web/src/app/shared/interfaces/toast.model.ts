export interface ToastData {
  title: string;
  copy: string;
  status: 'success' | 'failed' | 'inProgress';
}