import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError, throwError, BehaviorSubject } from 'rxjs';

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: string;
  id_token: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private AUTH_ENDPOINT = 'https://oauth.nhsnlink.org/auth/realms/NHSNLink/protocol/openid-connect';

  private authStatus = new BehaviorSubject<boolean>(this.isLoggedIn())

  constructor(private http: HttpClient, private router: Router) { }

  /* Methods */

  // We will be reading the base urls from .env files in the future.
  private getBaseURL() {
    let baseUrl = window.location.protocol + '//' + window.location.hostname;
    // for adding the localhost port. We will be reading the base urls from .env files in the future.
    if (window.location.port) {
      baseUrl += ':' + window.location.port;
    }
    return baseUrl;
  }

  // Login method that would return the auth url.
  login() {
    // Redirect to Keycloak login page
    return `${this.AUTH_ENDPOINT}/auth?client_id=nhsnlink-app&response_type=code&redirect_uri=${this.getBaseURL()}/callback&scope=openid%20profile%20email`;
  }

  // Using the code from redirect url, below method will get a valid token.
  getToken(code: string) {
    const payload = new URLSearchParams();
    payload.set('client_id', 'nhsnlink-app');
    payload.set('scope', 'openid profile email');
    payload.set('code', code);
    payload.set('redirect_uri', this.getBaseURL() + '/callback');
    payload.set('grant_type', 'authorization_code');

    return this.http.post<TokenResponse>(`${this.AUTH_ENDPOINT}/token`, payload.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
  }

  // This method will be used to get a new refresh token.
  refreshToken() {
    const refreshToken = sessionStorage.getItem('refresh_token');
    if (!refreshToken) {
      // Handle the absence of a refresh token
      return;
    }

    const payload = new URLSearchParams();
    payload.set('client_id', 'your_client_id');
    payload.set('refresh_token', refreshToken);
    payload.set('grant_type', 'refresh_token');

    return this.http.post<TokenResponse>(`${this.AUTH_ENDPOINT}/token`, payload.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    }).pipe(
      tap(response => {
        sessionStorage.setItem('access_token', response.access_token);
        // Update refresh token if it's returned in the response
        if (response.refresh_token) {
          sessionStorage.setItem('refresh_token', response.refresh_token);
        }
      }),
      catchError(error => {
        // Handle errors (e.g., redirect to login)
        return throwError(() => error);
      })
    );
  }

  // This method will call the getToken and cache the response values.
  handleAuth(code: string) {
    debugger;
    return this.getToken(code).pipe(
      tap(response => {
        // Store tokens in a secure storage
        sessionStorage.setItem('access_token', response.access_token);
        sessionStorage.setItem('refresh_token', response.refresh_token);
        sessionStorage.setItem('expires_in', JSON.stringify(new Date().getTime() + (Number(response.expires_in) * 1000)));
        sessionStorage.setItem('id_token', response.id_token);
        this.updateAuthStatus(true)
      })
    );
  }

  // Checks if the user is logged in by checking if we have valid access token.
  isLoggedIn(): boolean {
    const accessToken = sessionStorage.getItem('access_token');
    const expiresIn = sessionStorage.getItem('expires_in');
    if (!accessToken || !expiresIn) {
      return false;
    }
    return new Date().getTime() < JSON.parse(expiresIn);
  }

  get isLoggedIn$() {
    return this.authStatus.asObservable()
  }

  updateAuthStatus(isLoggedIn: boolean) {
    this.authStatus.next(isLoggedIn)
  }

  // Logs out the user by clearing the access_token
  logout(): void {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('expires_in');
    sessionStorage.removeItem('id_token');
    sessionStorage.removeItem('refresh_token');

    const logoutUrl = `${this.AUTH_ENDPOINT}/logout?redirect_uri=${this.getBaseURL()}`;
    window.location.href = logoutUrl;
  }

  // Checks if the token is expiring
  isTokenExpiring(): boolean {
    const expiresIn = sessionStorage.getItem('expires_in');
    const bufferTime = 300000; // 5 minute buffer
    if (!expiresIn) {
      return false;
    }
    return new Date().getTime() > JSON.parse(expiresIn) - bufferTime;
  }
}
