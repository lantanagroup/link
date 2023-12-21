import { ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from 'src/services/auth/auth.service';

export function authGuard(router: Router, authService: AuthService): (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => boolean {
  return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
    if (authService.isLoggedIn()) {
      return true;
    } else {
      router.navigate(['/login']);
      return false;
    }
  };
}
