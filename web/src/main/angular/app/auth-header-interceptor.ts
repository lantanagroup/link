import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpXsrfTokenExtractor} from '@angular/common/http';
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

    constructor(private authService: AuthService, private configService: ConfigService, private csrfTokenExtractor: HttpXsrfTokenExtractor) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        let headers = req.headers;
        if (this.configService.config && req.url.startsWith(this.configService.config.apiUrl)) {

            if (this.authService.getAuthToken()) {
              headers = headers.set('Authorization', 'Bearer ' + this.authService.token);
              headers = headers.set('X-Requested-With', 'XMLHttpRequest');

              // Send request with credential options in order to be able to read cross-origin cookies
              // If this is not set and a cross-origin request is made, then the XSRF token will be null
              // https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/withCredentials
              // https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
              req = req.clone({ withCredentials: true });

              //Add CSRF Token if needed
              if(req.method != "GET" && req.method != "HEAD" && req.method != "OPTIONS") {
                const csrfHeaderName = 'X-XSRF-TOKEN';
                let csrfToken = this.csrfTokenExtractor.getToken() as string;
                if (csrfToken !== null && !req.headers.has(csrfHeaderName)) {
                  headers = headers.set(csrfHeaderName, csrfToken);
                }
              }
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
                            let token = this.authService.getAuthToken();
                            req = req.clone({
                                headers: req.headers
                                    .set('Authorization', `Bearer ${token}`)
                                    .set('Cache-Control', 'no-cache')
                                    .set('X-Requested-With', 'XMLHttpRequest')
                                    .set('fhirBase', this.authService.fhirBase)
                            });
                            return next.handle(req);
                        })
                    );
                }
                else{
                    throw error;
                }
            })
        );
    }
}
