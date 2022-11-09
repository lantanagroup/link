import {Component, Input, OnInit, Output} from '@angular/core';
import {Subject} from 'rxjs';
import {EnvironmentService} from '../environment.service';
import {BundlerConfig} from '../models/config';

@Component({
  selector: 'app-bundler',
  templateUrl: './bundler.component.html',
  styleUrls: ['./bundler.component.css']
})
export class BundlerComponent implements OnInit {
  @Input() bundlerConfig: BundlerConfig;
  @Output() change = new Subject();

  constructor(public envService: EnvironmentService) { }

  ngOnInit(): void {
  }

}
