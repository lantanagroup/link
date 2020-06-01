import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {SelectLocationsComponent} from './select-locations/select-locations.component';
import {CookieService} from 'ngx-cookie-service';
import {OAuthModule} from 'angular-oauth2-oidc';
import {AddHeaderInterceptor} from './auth-header-interceptor';
import {ToastService} from './toast.service';
import {ToastsContainerComponent} from './toasts-container/toasts-container.component';
import { SmartLoginComponent } from './smart-login/smart-login.component';
import { HomeComponent } from './home/home.component';
import { QuestionnaireComponent } from './questionnaire/questionnaire.component';
import {AuthService} from './auth.service';
import { SmartHomeComponent } from './smart-home/smart-home.component';

@NgModule({
    declarations: [
        AppComponent,
        SelectLocationsComponent,
        ToastsContainerComponent,
        SmartLoginComponent,
        HomeComponent,
        QuestionnaireComponent,
        SmartHomeComponent
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
        ToastService,
        AuthService,
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
