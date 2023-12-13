import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { Router, RouterModule } from '@angular/router';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { CallbackComponent } from 'src/callback/callback.component';
import { AuthInterceptor } from './services/auth.interceptor';
import { authGuard } from './services/auth.guard';
import { AuthService } from './services/auth.service';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    {
      provide: 'AuthGuard',
      useFactory: (router: Router, authService: AuthService) => authGuard(router, authService),
      deps: [Router, AuthService]
    },
    importProvidersFrom(RouterModule.forRoot([
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
        path: 'facilities',
        loadComponent: () => import('./app/pages/facilities/facilities.component').then(m => m.FacilitiesComponent),
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
        path: 'system-performance',
        loadComponent: () => import('./app/pages/system-performance/system-performance.component').then(m => m.SystemPerformanceComponent),
        canActivate: ['AuthGuard']
      },
    ])),
    importProvidersFrom(HttpClientModule)
  ]
}).catch(err => console.error(err));
