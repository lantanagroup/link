import {Pipe, PipeTransform} from '@angular/core';
import {stringify} from 'yaml';

@Pipe({
  name: 'yaml',
  pure: false
})
export class YamlPipe implements PipeTransform {

  private stringToSnakeCase(inputString: string) {
    return inputString.split('').map((character) => {
      if (character == character.toUpperCase()) {
        return '-' + character.toLowerCase();
      } else {
        return character;
      }
    })
      .join('');
  }


  private stringToCamelCase(s: string) {
    return s.replace(/([-_][a-z])/ig, ($1) => {
      return $1.toUpperCase()
        .replace('-', '')
        .replace('_', '');
    });
  }


  public objectToSnakeCase(obj: any) {
    const keys = Object.keys(obj);

    for (let i = 0; i < keys.length; i++) {
      const current = keys[i];
      const newKey = this.stringToSnakeCase(current);

      if (current !== newKey) {
        obj[newKey] = obj[current];
        delete obj[current];
      }

      if (obj[newKey] instanceof Array) {
        for (let next of obj[newKey]) {
          if (typeof next === 'object') {
            this.objectToSnakeCase(next);
          }
        }
      } else if (typeof obj[newKey] === 'object') {
        this.objectToSnakeCase(obj[newKey]);
      }
    }
  }


  public objectToCamelCase(obj: any) {
    const keys = Object.keys(obj);

    for (let i = 0; i < keys.length; i++) {
      const current = keys[i];
      const newKey = this.stringToCamelCase(current);

      if (current !== newKey) {
        obj[newKey] = obj[current];
        delete obj[current];
      }

      if (obj[newKey] instanceof Array) {
        for (let next of obj[newKey]) {
          if (typeof next === 'object') {
            this.objectToCamelCase(next);
          }
        }
      } else if (typeof obj[newKey] === 'object') {
        this.objectToCamelCase(obj[newKey]);
      }
    }
  }


  transform(value: any, ...args: unknown[]): string {
    const clone = JSON.parse(JSON.stringify(value));
    delete clone.configType;

    this.objectToSnakeCase(clone);

    return stringify(clone, { version: '1.2' });
  }
}
