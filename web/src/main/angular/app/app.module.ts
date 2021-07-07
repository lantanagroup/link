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
import {AuthService} from './services/auth.service';
import {ConfigService} from './services/config.service';
import {ReportService} from './services/report.service';
import {ReviewComponent} from "./review/review.component";
import {GenerateComponent} from "./generate/generate.component";
import {ReportComponent} from "./report/report.component";
import {NgbdDatepickerRangePopup} from "./components/datepicker-range-popup";
import {CalculatedFieldComponent} from './calculated-field/calculated-field.component';
import {NgbdDatepickerPopup} from "./components/datepicker-popup";
import {ReportDefinitionService} from './services/report-definition.service';

export const initFactory = (configService: ConfigService, authService: AuthService) => {
  return async () => {
    await configService.loadConfig();

    await authService.loginLocal();
  };
};

@NgModule({
  declarations: [
    AppComponent,
    ToastsContainerComponent,
    ReviewComponent,
    GenerateComponent,
    ReportComponent,
    CalculatedFieldComponent,
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
      useFactory: initFactory,
      deps: [ConfigService, AuthService],
      multi: true
    },
    ConfigService,
    ToastService,
    AuthService,
    ReportService,
    CookieService,
    ReportDefinitionService,
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
