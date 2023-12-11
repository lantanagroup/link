import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { RouterModule } from '@angular/router';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { CallbackComponent } from 'src/callback/callback.component';
import { AuthInterceptor } from './services/auth.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    importProvidersFrom(RouterModule.forRoot([
      { path: 'login',
        loadComponent: () => import('./app/login/login.component').then(m => m.LoginComponent)
      },
      { path: 'callback', component: CallbackComponent },
      {
        path: 'dashboard',
        loadComponent: () => import('./app/pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'activities',
        loadComponent: () => import('./app/pages/activities/activities.component').then(m => m.ActivitiesComponent)
      },
      {
        path: 'facilities',
        loadComponent: () => import('./app/pages/facilities/facilities.component').then(m => m.FacilitiesComponent)
      },
      {
        path: 'resources',
        loadComponent: () => import('./app/pages/resources/resources.component').then(m => m.ResourcesComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./app/pages/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'system-performance',
        loadComponent: () => import('./app/pages/system-performance/system-performance.component').then(m => m.SystemPerformanceComponent)
      },
    ])),
    importProvidersFrom(HttpClientModule)
  ]
}).catch(err => console.error(err));
