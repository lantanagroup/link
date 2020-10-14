import {Directive, ViewContainerRef} from '@angular/core';

@Directive({
  selector: '[reportBody]'
})
export class ReportBodyDirective {

  constructor(public viewContainerRef: ViewContainerRef) {
  }

}
