import {ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';

import {AuthService} from './auth.service';

@Injectable()
export class AuthGuard implements CanActivate, CanActivateChild {
    constructor(private authService: AuthService, private router: Router) {
    }

    canActivate(route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {

        let auth = !!this.authService.user;
        if (auth) {
            return true;
        } else {
            this.router.navigate(['/']);
        }
    }

    canActivateChild(route: ActivatedRouteSnapshot,
                     state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
        return this.canActivate(route, state);
    }
}
