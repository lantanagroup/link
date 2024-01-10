import { Pipe, PipeTransform } from "@angular/core"

@Pipe({ 
  name: 'idFromTitle',
  standalone: true
})
export class IdFromTitlePipe implements PipeTransform {
  transform(value: string): string {
    return value.toLowerCase().replace(/\s+/g, '-')
  }
}