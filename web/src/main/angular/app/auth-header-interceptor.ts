import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';
import {OAuthService} from 'angular-oauth2-oidc';

/**
 * This class is an HTTP interceptor that is responsible for adding an
 * Authorization header to every request sent to the application server.
 */
@Injectable()
export class AddHeaderInterceptor implements HttpInterceptor {
    constructor(private oauthService: OAuthService) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        let headers = req.headers;

        if (req.url.startsWith('/')) {
            if (this.oauthService.getIdToken()) {
                headers = headers.set('Authorization', 'Bearer ' + this.oauthService.getIdToken());
            }
        }

        return next.handle(req.clone({ headers: headers }));
    }
}