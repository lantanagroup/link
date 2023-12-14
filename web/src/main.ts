import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { Router, RouterModule } from '@angular/router';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { CallbackComponent } from 'src/callback/callback.component';
import { AuthInterceptor } from './services/auth.interceptor';
import { authGuard } from './services/auth.guard';
import { AuthService } from './services/auth.service';
import { DashboardComponent } from './app/pages/dashboard/dashboard.component';
import { LoginComponent } from './app/pages/login/login.component';
import { ActivitiesComponent } from './app/pages/activities/activities.component';
import { FacilitiesComponent } from './app/pages/facilities/facilities.component';
import { ResourcesComponent } from './app/pages/resources/resources.component';
import { ProfileComponent } from './app/pages/profile/profile.component';
import { SystemPerformanceComponent } from './app/pages/system-performance/system-performance.component';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    {
      provide: 'AuthGuard',
      useFactory: (router: Router, authService: AuthService) => authGuard(router, authService),
      deps: [Router, AuthService]
    },
    importProvidersFrom(RouterModule.forRoot([
      { path: '', redirectTo: '/login', pathMatch: 'full' }, //Redirects root path to login
      {
        // path: 'login',
        // loadComponent: () => import('./app/pages/login/login.component').then(m => m.LoginComponent),
        path: 'login', component: LoginComponent
      },
      { path: 'callback', component: CallbackComponent },
      {
        // path: 'dashboard',
        // loadComponent: () => import('./app/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        // canActivate: ['AuthGuard']
        path: 'dashboard', component: DashboardComponent, canActivate: ['AuthGuard']
      },
      {
        // path: 'activities',
        // loadComponent: () => import('./app/pages/activities/activities.component').then(m => m.ActivitiesComponent),
        // canActivate: ['AuthGuard']
        path: 'activities', component: ActivitiesComponent, canActivate: ['AuthGuard']
      },
      {
        // path: 'facilities',
        // loadComponent: () => import('./app/pages/facilities/facilities.component').then(m => m.FacilitiesComponent),
        // canActivate: ['AuthGuard']
        path: 'facilities', component: FacilitiesComponent, canActivate: ['AuthGuard']
      },
      {
        // path: 'resources',
        // loadComponent: () => import('./app/pages/resources/resources.component').then(m => m.ResourcesComponent),
        // canActivate: ['AuthGuard']
        path: 'resources', component: ResourcesComponent, canActivate: ['AuthGuard']
      },
      {
        // path: 'profile',
        // loadComponent: () => import('./app/pages/profile/profile.component').then(m => m.ProfileComponent),
        // canActivate: ['AuthGuard']
        path: 'profile', component: ProfileComponent, canActivate: ['AuthGuard']
      },
      {
        // path: 'system-performance',
        // loadComponent: () => import('./app/pages/system-performance/system-performance.component').then(m => m.SystemPerformanceComponent),
        // canActivate: ['AuthGuard']
        path: 'system-performance', component: SystemPerformanceComponent, canActivate: ['AuthGuard']
      },
    ])),
    importProvidersFrom(HttpClientModule)
  ]
}).catch(err => console.error(err));
