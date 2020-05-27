import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OAuthService} from 'angular-oauth2-oidc';
import {AuthService} from './auth.service';

/**
 * This class is an HTTP interceptor that is responsible for adding an
 * Authorization header to every request sent to the application server.
 */
@Injectable()
export class AddHeaderInterceptor implements HttpInterceptor {
    constructor(private authService: AuthService) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        let headers = req.headers;

        if (req.url.startsWith('/')) {
            if (this.authService.token) {
                headers = headers.set('Authorization', 'Bearer ' + this.authService.token);
            }
            if (this.authService.fhirBase) {
                headers = headers.set('fhirBase', this.authService.fhirBase);
            }
        }

        return next.handle(req.clone({ headers: headers }));
    }
}