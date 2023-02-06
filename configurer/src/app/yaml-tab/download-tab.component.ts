import {Component, Input, OnInit} from '@angular/core';
import {saveAs} from 'file-saver';
import {ToastService} from "../toast.service";
import {faCopy, faDownload} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-download-tab',
  templateUrl: './download-tab.component.html',
  styleUrls: ['./download-tab.component.css']
})
export class DownloadTabComponent implements OnInit {
  @Input() config: any;
  @Input() format?: 'yaml'|'json' = 'yaml';
  faDownload = faDownload;
  faCopy = faCopy;

  constructor(private toastService: ToastService) { }

  download(value: string) {
    const blob = new Blob([value], { type: 'text/yaml' });
    saveAs(blob, 'config.yaml');
  }

  copyToClipboard(value: string){
    const selBox = document.createElement('textarea');
    selBox.style.position = 'fixed';
    selBox.style.left = '0';
    selBox.style.top = '0';
    selBox.style.opacity = '0';
    selBox.value = value;
    document.body.appendChild(selBox);
    selBox.focus();
    selBox.select();
    document.execCommand('copy');
    document.body.removeChild(selBox);

    this.toastService.show('Copied!', 'Content copied to clipboard!', 3000);
  }

  ngOnInit(): void {
  }

}
