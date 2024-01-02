import { Component, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NavigationEnd, Router, Event as RouterEvent, RouterOutlet } from '@angular/router';

import { HeaderComponent } from './shared/header/header.component';

import { FooterComponent } from './shared/footer/footer.component';
import { filter } from 'rxjs';
import { LoginComponent } from './pages/login/login.component';
import { ToastComponent } from './shared/toast/toast.component';


@Component({
    selector: 'app-root',
    standalone: true,
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [CommonModule, HttpClientModule, LoginComponent, HeaderComponent, FooterComponent, RouterOutlet, ToastComponent]
})
export class AppComponent {
  title = 'web';
  showHeaderAndFooter = true;

  /**
  * Based on the navigated route, the constructor decides whether to show or hide the header and footer.
  * For instance, the header and footer are hidden when navigating to the login page.
  */
  constructor(private router: Router, private renderer: Renderer2) {
    this.router.events.pipe(
      filter((event: RouterEvent): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.showHeaderAndFooter = !event.urlAfterRedirects.startsWith('/login');
    });
  }

  /**
   * Adding Bugherd script to dev and staging environments
   * todo : Remove before final deployment
   */

  ngOnInit(): void {
    if(window.location.hostname !== 'nhsnlink.org') {
      const bugherdScript = this.renderer.createElement('script')

      this.renderer.setAttribute(bugherdScript, 'src', 'https://www.bugherd.com/sidebarv2.js?apikey=l0lk84x9ox3ahezpcyc6hw')

      this.renderer.appendChild(document.head, bugherdScript)
    }
  }
}
