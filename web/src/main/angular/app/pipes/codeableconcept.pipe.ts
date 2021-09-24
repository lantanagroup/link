import {Pipe, PipeTransform} from '@angular/core';
import {ICodeableConcept} from "../fhir";

@Pipe({
  name: 'codeableConcept'
})
export class CodeableConceptPipe implements PipeTransform {

  transform(codeableConcept: ICodeableConcept, firstCode = false, defaultValue = ''): string {
    if (!codeableConcept) return defaultValue;

    if (firstCode) {
      if (codeableConcept.coding && codeableConcept.coding.length > 0 && codeableConcept.coding[0].code) {
        return codeableConcept.coding[0].code;
      }
    } else {
      if (codeableConcept.text) {
        return codeableConcept.text;
      } else if (codeableConcept.coding && codeableConcept.coding.length > 0) {
        if (codeableConcept.coding[0].display) {
          return codeableConcept.coding[0].display;
        } else if (codeableConcept.coding[0].code) {
          return codeableConcept.coding[0].code;
        }
      }
    }

    return defaultValue;
  }
}
