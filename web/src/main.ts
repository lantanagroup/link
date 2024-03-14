import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { Router, RouterModule } from '@angular/router';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';

import { AuthInterceptor } from './services/auth/auth.interceptor';
import { authGuard } from './services/auth/auth.guard';
import { AuthService } from './services/auth/auth.service';
import { CallbackComponent } from './callback/callback.component';

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
        path: 'login',
        loadComponent: () => import('./app/pages/login/login.component').then(m => m.LoginComponent),
      },
      { path: 'callback', component: CallbackComponent },
      {
        path: 'dashboard',
        loadComponent: () => import('./app/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'activities',
        loadComponent: () => import('./app/pages/activities/activities.component').then(m => m.ActivitiesComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'activities/bundle/:tenantId/:bundleId',
        loadComponent: () => import('./app/pages/activities/bundle/bundle.component').then(m => m.BundleComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'facilities',
        loadComponent: () => import('./app/pages/facilities/facilities.component').then(m => m.FacilitiesComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'facilities/add-facility',
        loadComponent: () => import('./app/pages/facilities/add-facility/add-facility.component').then(m => m.AddFacilityComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'facilities/facility/:id',
        loadComponent: () => import('./app/pages/facilities/facility/facility.component').then(m => m.FacilityComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'facilities/edit-facility/:id',
        loadComponent: () => import('./app/pages/facilities/edit-facility/edit-facility.component').then(m => m.EditFacilityComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'resources',
        loadComponent: () => import('./app/pages/resources/resources.component').then(m => m.ResourcesComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'profile',
        loadComponent: () => import('./app/pages/profile/profile.component').then(m => m.ProfileComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'profile/update-password',
        loadComponent: () => import('./app/pages/profile/update-password/update-password.component').then(m => m.UpdatePasswordComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'profile/edit-profile',
        loadComponent: () => import('./app/pages/profile/edit-profile/edit-profile.component').then(m => m.EditProfileComponent),
        canActivate: ['AuthGuard']
      },
      {
        path: 'system-performance',
        loadComponent: () => import('./app/pages/system-performance/system-performance.component').then(m => m.SystemPerformanceComponent),
        canActivate: ['AuthGuard']
      },
    ])),
    importProvidersFrom(HttpClientModule)
  ]
}).catch(err => console.error(err));
