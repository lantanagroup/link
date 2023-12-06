import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';

import { HeaderComponent } from './shared/header/header.component';
import { LoginComponent } from 'src/app/login/login.component';
import { TenantReportsComponent } from './tenant-reports/tenant-reports.component';
import { FooterComponent } from './shared/footer/footer.component';


@Component({
    selector: 'app-root',
    standalone: true,
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [CommonModule, HttpClientModule, LoginComponent, TenantReportsComponent, HeaderComponent, FooterComponent, RouterOutlet]
})
export class AppComponent {
  title = 'web';
}
