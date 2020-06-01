import { Component, OnInit } from '@angular/core';
import {AuthService} from '../auth.service';

@Component({
  selector: 'app-smart-home',
  templateUrl: './smart-home.component.html',
  styleUrls: ['./smart-home.component.css']
})
export class SmartHomeComponent implements OnInit {

  constructor(public authService: AuthService) { }

  copyAccessTokenToClipboard() {
    const selBox = document.createElement('textarea');
    selBox.style.position = 'fixed';
    selBox.style.left = '0';
    selBox.style.top = '0';
    selBox.style.opacity = '0';
    selBox.value = this.authService.oauthService.getAccessToken();
    document.body.appendChild(selBox);
    selBox.focus();
    selBox.select();
    document.execCommand('copy');
    document.body.removeChild(selBox);
  }

  ngOnInit(): void {
  }

}
