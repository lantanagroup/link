import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'jsonConfig'
})
export class JsonConfigPipe implements PipeTransform {

  transform(value: any, ...args: unknown[]): unknown {
    const clone = JSON.parse(JSON.stringify(value));
    delete clone.configType;

    return JSON.stringify(clone, null, '\t');
  }
}
