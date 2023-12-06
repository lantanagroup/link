import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { RouterModule } from '@angular/router';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { LoginComponent } from 'src/app/login/login.component';
import { CallbackComponent } from 'src/callback/callback.component';
import { AuthInterceptor } from './services/auth.interceptor';
import { DashboardComponent } from './app/pages/dashboard/dashboard.component';
import { ActivitiesComponent } from './app/pages/activities/activities.component';
import { FacilitiesComponent } from './app/pages/facilities/facilities.component';
import { ResourcesComponent } from './app/pages/resources/resources.component';
import { ProfileComponent } from './app/pages/profile/profile.component';
import { SystemPerformanceComponent } from './app/pages/system-performance/system-performance.component';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    importProvidersFrom(RouterModule.forRoot([
      { path: 'login', component: LoginComponent },
      { path: 'callback', component: CallbackComponent },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'activities', component: ActivitiesComponent },
      { path: 'facilities', component: FacilitiesComponent },
      { path: 'resources', component: ResourcesComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'system-performance', component: SystemPerformanceComponent },
      // ... other routes
    ])),
    importProvidersFrom(HttpClientModule)
  ]
}).catch(err => console.error(err));
