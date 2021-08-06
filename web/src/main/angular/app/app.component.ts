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

  get buildInfo() {
    if (!this.apiInfo) return;

    if (this.apiInfo.version && this.apiInfo.build) {
      return `${this.apiInfo.version} (build: ${this.apiInfo.build})`;
    } else if (this.apiInfo.build) {
      return `build: ${this.apiInfo.build}`;
    } else if (this.apiInfo.version) {
      return this.apiInfo.version;
    } else {
      return 'Unexpected build info';
    }
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

  async ngOnInit() {
    this.configService.getApiInfo()
        .then((apiInfo: ApiInfoModel) => this.apiInfo = apiInfo);
  }
}
