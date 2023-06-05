import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {GenerateComponent} from './generate/generate.component';
import {ReviewComponent} from "./review/review.component";
import {ReportComponent} from "./report/report.component";
import {ErrorPageComponent} from './error-page/error-page.component';
import {UnauthorizedComponent} from "./unauthorized/unauthorized.component";

const routes: Routes = [
  {path: '', redirectTo: 'generate', pathMatch: 'full'},
  {path: 'generate', component: GenerateComponent},
  {path: 'review', component: ReviewComponent},
  {path: 'review/:id', component: ReportComponent},
  {path: 'not-found', component: ErrorPageComponent, data: {message: 'Page not found!'}},
  {path: 'unauthorized', component: UnauthorizedComponent, data: {message: 'Authorization has failed while attempting to perform the previous request. Please check that you have a authenticated session or the appropriate roles within LINK.'}},
  {path: '**', redirectTo: '/not-found'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { relativeLinkResolution: 'legacy' })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
