import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppData } from '../interfaces/app.model';
import { AppApiService } from 'src/services/api/app/app-api.service';

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

  constructor(
    private appApiService: AppApiService
  ) {}

  async ngOnInit() {
    try {
      this.appData = await this.appApiService.fetchAppData()
    } catch (error) {
      console.error('Error loading version data:', error)
    }
  }

}
