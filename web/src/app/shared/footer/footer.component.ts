import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppData } from '../interfaces/app.model';
import { AppApiService } from 'src/services/api/app/app-api.service';
import { AuthService } from 'src/services/auth/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent {

  copyrightYear: number = new Date().getFullYear();
  appData: AppData | null = null;

  private authSubscription!: Subscription;

  constructor(
    private appApiService: AppApiService,
    private authService: AuthService
  ) {}

  async ngOnInit() {
    this.authSubscription = this.authService.isLoggedIn$.subscribe(
      async (isLoggedIn) => {
        if (isLoggedIn) {
          try {
            this.appData = await this.appApiService.fetchAppData()
          } catch (error) {
            console.error('Error loading version data:', error)
          }
        }
      }
    )
  }

}
