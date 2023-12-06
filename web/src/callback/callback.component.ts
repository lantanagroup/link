import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-callback',
  template: `<p>Processing login...</p>`,
  standalone: true,
})
export class CallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    console.log(code);
    // debugger;
    if (code) {
      this.authService.handleAuth(code).subscribe({
        next: () => {
          // Navigate to the desired route after successful login
          this.router.navigate(['/secure']);
        },
        error: (error) => {
          console.error('Authentication error:', error);
          // Optionally navigate to an error page or login page
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
