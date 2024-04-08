import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';
import { GlobalApiService } from 'src/services/api/globals/globals-api.service';
import { jwtDecode } from 'jwt-decode';

@Component({
  selector: 'app-callback',
  template: `<p>Processing login...</p>`,
  standalone: true,
})
export class CallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router,
    private globalApiService: GlobalApiService
  ) { }

  async ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (code) {
      this.authService.handleAuth(code).subscribe({
        next: async () => {
          // Check to see if Keycloak authenticated user has NHSNLink access
          const accessToken = sessionStorage.getItem('access_token')

          if(accessToken) {
            const decodedToken: any = jwtDecode(accessToken)
            try {
              // not using user details currently, using this call to authenticate the keycloak user against NHSNLink access
              const userDetails = await this.globalApiService.getContent(`user?email=${decodedToken?.email}`)
              // Navigate to the desired route after successful login
              // TODO: Handle browser cookies code here:
              // Research on HTTP only browser cookies.
              this.router.navigate(['/dashboard']);

            } catch (error: any) {

              console.warn('NHSNLink authentication error:', error)
              const errorMessage = encodeURIComponent('<h6 class="text-warning">There has been an error</h6><em>You are not authorized to use this application, contact your system administrator.</em>')
              this.authService.logout(errorMessage);
            }
          }
        },
        error: (error) => {
          console.error('Authentication error:', error);
          // Optionally navigate to an error page or login page
          sessionStorage.setItem('loginMessage', error.message)
          this.router.navigate(['/login']);
        }
      });
    } else {
      // Handle the absence of a code in the URL
      // Redirect to login or show an error message
      this.router.navigate(['/login']);
    }
  }
}
