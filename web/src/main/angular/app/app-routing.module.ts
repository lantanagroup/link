import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {SmartLoginComponent} from './smart-login/smart-login.component';
import {SmartHomeComponent} from './smart-home/smart-home.component';
import {GenerateComponent} from "./generate/generate.component";
import {ReviewComponent} from "./review/review.component";
import {ReportComponent} from "./report/report.component";

const routes: Routes = [
    {path: 'smart-login', component: SmartLoginComponent},
    {path: 'generate',  component: GenerateComponent},
    {path: 'review', component: ReviewComponent, children: [{path: ':id', component: ReportComponent}]},
    {path: 'smart-home', component: SmartHomeComponent},
    {path: '', redirectTo: 'generate', pathMatch: 'full'}
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
