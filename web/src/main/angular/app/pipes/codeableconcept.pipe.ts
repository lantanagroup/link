import {Pipe, PipeTransform} from '@angular/core';
import {ICodeableConcept} from "../fhir";

@Pipe({
  name: 'codeableConcept'
})
export class CodeableConceptPipe implements PipeTransform {

  transform(codeableConcept: ICodeableConcept): string {

    if (codeableConcept.text) {
      return codeableConcept.text;
    } else if (codeableConcept.coding && codeableConcept.coding.length == 1 && codeableConcept.coding[0].display) {
      return codeableConcept.coding[0].display;
    } else {
      return "unknown";
    }
  }
}
