import {Pipe, PipeTransform} from '@angular/core';
import {SimpleQuantity} from "../model/fhir";

@Pipe({
  name: 'simpleQuantity'
})
export class SimplequantityPipe implements PipeTransform {

  transform(simpleQuantity: SimpleQuantity, defaultValue = ''): string {

    if (simpleQuantity.value && simpleQuantity.unit) {
      return simpleQuantity.value.toString() + " " + simpleQuantity.unit;
    } else if (simpleQuantity.value) {
      return simpleQuantity.value.toString()
    }
    return defaultValue;
  }
}
