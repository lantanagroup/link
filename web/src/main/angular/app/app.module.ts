import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";
import {HttpClientModule} from "@angular/common/http";
import {AgmCoreModule} from "@agm/core";
import {AgmOverlay, AgmOverlays} from "agm-overlays";
import {AgmJsMarkerClustererModule} from "@agm/js-marker-clusterer";
import {FormsModule} from "@angular/forms";
import { SelectLocationsComponent } from './select-locations/select-locations.component';
import {CookieService} from "ngx-cookie-service";

@NgModule({
  declarations: [
    AppComponent,
    SelectLocationsComponent
  ],
    imports: [
        BrowserModule,
        AppRoutingModule,
        NgbModule,
        HttpClientModule,
        AgmOverlays,
        AgmCoreModule.forRoot({
            apiKey: '%google.api.key%',
            libraries: ['visualization']
        }),
        AgmJsMarkerClustererModule,
        FormsModule
    ],
  providers: [
    CookieService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
