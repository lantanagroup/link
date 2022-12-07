import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpXsrfTokenExtractor} from '@angular/common/http';
import {from, Observable} from 'rxjs';
import {AuthService} from './services/auth.service';
import {ConfigService} from './services/config.service';
import {catchError, switchMap} from "rxjs/operators";
import {ToastService} from "./toast.service";
import {Router} from "@angular/router";

/**
 * This class is an HTTP interceptor that is responsible for adding an
 * Authorization header to every request sent to the application server.
 */
@Injectable()
export class AddHeaderInterceptor implements HttpInterceptor {

    constructor(public toastService: ToastService, private router: Router, private authService: AuthService, private configService: ConfigService, private csrfTokenExtractor: HttpXsrfTokenExtractor) {
    }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        let headers = req.headers;
        if (this.configService.config && req.url.startsWith(this.configService.config.apiUrl)) {

            if (this.authService.getAuthToken()) {
              headers = headers.set('Authorization', 'Bearer ' + this.authService.token);
              //headers = headers.set('X-Requested-With', 'XMLHttpRequest');

              // Send request with credential options in order to be able to read cross-origin cookies
              // If this is not set and a cross-origin request is made, then the XSRF token will be null
              // https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/withCredentials
              // https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
              //req = req.clone({ withCredentials: true });


              //Add CSRF Token if needed
              /*
              if(req.method != "GET" && req.method != "HEAD" && req.method != "OPTIONS") {
                const csrfHeaderName = 'X-XSRF-TOKEN';
                let csrfToken = this.csrfTokenExtractor.getToken() as string;
                if (csrfToken !== null && !req.headers.has(csrfHeaderName)) {
                  headers = headers.set(csrfHeaderName, csrfToken);
                }
                else {
                  //debug for CSRF token issues
                  console.log(`Cookies available to the client: ${document.cookie.split(";")}`);
                  console.log(`CSRF TOKEN VALUE: ${csrfToken}. Token not getting applied.`)
                }
              }
              */
            }
            if (this.authService.fhirBase) {
                headers = headers.set('Cache-Control', 'no-cache');
                headers = headers.set('fhirBase', this.authService.fhirBase);
            }
        }

        return next.handle((req.clone({ headers: headers }))).pipe(
            catchError((error) => {
              if (error.status === 401) {
                this.toastService.showException('Unauthorized', error);
                this.router.navigate(['/unauthorized']);
                throw error;
              }
              else if (error.status === 403) {
                this.toastService.showException('Access Forbidden', error);
                this.router.navigate(['/unauthorized']);
                throw error;
              }
              else{
                //this.toastService.showException('Server Error', error);
                throw error;
              }
            })
        );
    }
}
