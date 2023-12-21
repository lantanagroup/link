import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Observable, throwError, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const accessToken = localStorage.getItem('access_token');

    if (!accessToken) {
      return next.handle(req);
    }

    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${accessToken}`)
    });

    if (this.authService.isTokenExpiring()) {
      const refreshTokenObservable = this.authService.refreshToken();

      if (refreshTokenObservable) {
        return refreshTokenObservable.pipe(
          switchMap(response => {
            if (response && response.access_token) {
              localStorage.setItem('access_token', response.access_token);
              const newAuthReq = req.clone({
                headers: req.headers.set('Authorization', `Bearer ${response.access_token}`)
              });
              return next.handle(newAuthReq);
            } else {
              return next.handle(authReq);
            }
          }),
          catchError(error => {
            return throwError(() => error);
          })
        );
      } else {
        alert("Could not regain the sesseion. Please login again.");
        this.authService.logout();
      }
    }

    return next.handle(authReq);
  }
}
