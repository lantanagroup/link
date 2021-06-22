import {BrowserModule} from '@angular/platform-browser';
import {APP_INITIALIZER, NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {CookieService} from 'ngx-cookie-service';
import {OAuthModule} from 'angular-oauth2-oidc';
import {AddHeaderInterceptor} from './auth-header-interceptor';
import {ToastService} from './toast.service';
import {ToastsContainerComponent} from './toasts-container/toasts-container.component';
import {SmartLoginComponent} from './smart-login/smart-login.component';
import {HomeComponent} from './home/home.component';
import {ReportBodyComponent} from './report-body/report-body.component';
import {AuthService} from './services/auth.service';
import {SmartHomeComponent} from './smart-home/smart-home.component';
import {ConfigService} from './services/config.service';
import {ReportService} from './services/report.service';
import {ReportBodyDirective} from './report-body.directive';
import {ReviewComponent} from "./review/review.component";
import {GenerateComponent} from "./generate/generate.component";
import {ReportComponent} from "./report/report.component";
import {NgbdDatepickerRangePopup} from "./components/datepicker-range-popup";
import {NgbdDatepickerPopup} from "./components/datepicker-popup";

export const configFactory = (configService: ConfigService) => {
    return () => configService.loadConfig();
};

@NgModule({
    declarations: [
        AppComponent,
        ToastsContainerComponent,
        SmartLoginComponent,
        HomeComponent,
        ReportBodyComponent,
        SmartHomeComponent,
        ReportBodyDirective,
        ReviewComponent,
        GenerateComponent,
        ReportComponent,
        NgbdDatepickerRangePopup,
        NgbdDatepickerPopup
    ],
    imports: [
        BrowserModule,
        AppRoutingModule,
        NgbModule,
        HttpClientModule,
        FormsModule,
        OAuthModule.forRoot()
    ],
    providers: [
        {
            provide: APP_INITIALIZER,
            useFactory: configFactory,
            deps: [ConfigService],
            multi: true
        },
        ConfigService,
        ToastService,
        AuthService,
        ReportService,
        CookieService,
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AddHeaderInterceptor,
            multi: true
        }
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
