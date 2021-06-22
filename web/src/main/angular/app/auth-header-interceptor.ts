import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {from, Observable} from 'rxjs';
import {AuthService} from './services/auth.service';
import {ConfigService} from './services/config.service';
import {catchError, switchMap} from "rxjs/operators";

/**
 * This class is an HTTP interceptor that is responsible for adding an
 * Authorization header to every request sent to the application server.
 */
@Injectable()
export class AddHeaderInterceptor implements HttpInterceptor {

    constructor(private authService: AuthService, private configService: ConfigService) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        let headers = req.headers;
        if (this.configService.config && req.url.startsWith(this.configService.config.apiUrl)) {
            if (this.authService.token) {
                headers = headers.set('Authorization', 'Bearer ' + this.authService.token);
            }
            if (this.authService.fhirBase) {
                headers = headers.set('Cache-Control', 'no-cache');
                headers = headers.set('fhirBase', this.authService.fhirBase);
            }
        }
        return next.handle((req.clone({ headers: headers }))).pipe(
            catchError((error) => {
                if (error.status === 403) {
                    return from(this.authService.loginLocal()).pipe(
                        switchMap(() => {
                            let token = this.authService.token;
                            req = req.clone({
                                headers: req.headers.set('Authorization', `Bearer ${token}`)
                            });
                            return next.handle(req);
                        })
                    );
                }
            })
        );
    }
}
