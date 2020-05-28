import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {SmartLoginComponent} from './smart-login/smart-login.component';
import {HomeComponent} from './home/home.component';

const routes: Routes = [{
  path: 'smart-login',
  component: SmartLoginComponent
}, {
  path: 'home',
  component: HomeComponent
}, {
  path: '',
  redirectTo: 'home',
  pathMatch: 'full'
}];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
