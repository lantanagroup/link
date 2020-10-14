import { Component, OnInit } from '@angular/core';
import {AuthService} from '../services/auth.service';

@Component({
  selector: 'app-smart-home',
  templateUrl: './smart-home.component.html',
  styleUrls: ['./smart-home.component.css']
})
export class SmartHomeComponent implements OnInit {

  constructor(public authService: AuthService) { }

  ngOnInit(): void {
  }

}
