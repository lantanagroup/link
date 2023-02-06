import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {FormsModule} from "@angular/forms";
import {ApiComponent} from './api/api.component';
import {ConsumerComponent} from './consumer/consumer.component';
import {AgentComponent} from './agent/agent.component';
import {WebComponent} from './web/web.component';
import {HttpClientModule} from "@angular/common/http";
import {HomeComponent} from './home/home.component';
import {YamlPipe} from './yaml.pipe';
import {SingleLineTextComponent} from './forms/single-line-text/single-line-text.component';
import {BooleanComponent} from './forms/boolean/boolean.component';
import {StringDropdownComponent} from './forms/string-dropdown/string-dropdown.component';
import {DownloadTabComponent} from './yaml-tab/download-tab.component';
import {CorsComponent} from './api/cors/cors.component';
import {ReportDefsComponent} from './api/report-defs/report-defs.component';
import {AuthConfigComponent} from './auth-config/auth-config.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {ToastsComponent} from './toasts/toasts.component';
import {ApiQueryComponent} from './api/query/api-query.component';
import {QueryComponent} from "./query/query.component";
import {JsonConfigPipe} from './json-config.pipe';
import {UscoreComponent} from './query/uscore/uscore.component';
import {ConceptMapsComponent} from './api/concept-maps/concept-maps.component';
import {FontAwesomeModule} from "@fortawesome/angular-fontawesome";
import {FhirSenderComponent} from './api/submission/fhir-sender/fhir-sender.component';
import {MeasureReportSenderComponent} from './api/submission/measure-report-sender/measure-report-sender.component';
import {DatastoreComponent} from './datastore/datastore.component';
import {EventComponent} from './api/events/event.component';
import {BundlerComponent} from './bundler/bundler.component';
import {MultiMeasureComponent} from './api/multi-measure/multi-measure.component';
import {UploadFileComponent} from './uploadfile/uploadfile.component';
import {FileUploadModule} from "ng2-file-upload";
import {SwaggerComponent} from './swagger/swagger.component';
import {APP_BASE_HREF} from '@angular/common';

@NgModule({
  declarations: [
    AppComponent,
    ApiComponent,
    ConsumerComponent,
    AgentComponent,
    WebComponent,
    HomeComponent,
    YamlPipe,
    SingleLineTextComponent,
    BooleanComponent,
    StringDropdownComponent,
    DownloadTabComponent,
    CorsComponent,
    ReportDefsComponent,
    AuthConfigComponent,
    ToastsComponent,
    ApiQueryComponent,
    QueryComponent,
    JsonConfigPipe,
    UscoreComponent,
    ConceptMapsComponent,
    FhirSenderComponent,
    MeasureReportSenderComponent,
    DatastoreComponent,
    EventComponent,
    BundlerComponent,
    MultiMeasureComponent,
    UploadFileComponent,
    SwaggerComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    NgbModule,
    FontAwesomeModule,
    FileUploadModule
  ],
  providers: [{provide: APP_BASE_HREF, useValue: '/configurer'}],
  bootstrap: [AppComponent]
})
export class AppModule { }
