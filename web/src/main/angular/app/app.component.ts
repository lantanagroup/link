import {Component, OnInit} from '@angular/core';
import {AuthService} from './services/auth.service';
import {ApiInfoModel} from './model/api-info-model';
import {ConfigService} from './services/config.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  public apiInfo: ApiInfoModel;

  constructor(public authService: AuthService, public configService: ConfigService) {

  }

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

  setFavicon() {
    const favIcon: HTMLLinkElement = document.querySelector('#favIcon');
    favIcon.href = `assets/${this.configService.config.faviconName !== undefined ? this.configService.config.faviconName : ''}favicon.ico`;
  }

  async ngOnInit() {
    this.setFavicon();
    this.configService.getApiInfo()
        .then((apiInfo: ApiInfoModel) => this.apiInfo = apiInfo);
  }

}
