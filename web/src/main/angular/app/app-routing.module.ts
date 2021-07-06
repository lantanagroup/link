import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {GenerateComponent} from "./generate/generate.component";
import {ReviewComponent} from "./review/review.component";
import {ReportComponent} from "./report/report.component";

const routes: Routes = [
  {path: 'generate', component: GenerateComponent},
  {path: 'review', component: ReviewComponent},
  {path: 'review/:id', component: ReportComponent},
  {path: '', redirectTo: 'generate', pathMatch: 'full'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
