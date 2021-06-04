import { Component, OnInit } from '@angular/core';
import {AuthService} from "../services/auth.service";

@Component({
  selector: 'nandina-review',
  templateUrl: './review.component.html',
  styleUrls: ['./review.component.css']
})
export class ReviewComponent implements OnInit {

  reports = [
    {
      id: 1,
      name: 'Report1'
    },
    {
      id: 2,
      name: 'Report2'
    },
    {
      id: 3,
      name: 'Report3'
    }
  ];

  constructor(public authService: AuthService) { }

  ngOnInit() {
  }

}
