import { Pipe, PipeTransform } from "@angular/core"

// ID from Title
@Pipe({ 
  name: 'idFromTitle',
  standalone: true
})
export class IdFromTitlePipe implements PipeTransform {
  transform(value: string): string {
    return value.toLowerCase().replace(/\s+/g, '-')
  }
}

// PascalCase to not 
@Pipe({
  name: 'pascalCaseToSpace',
  standalone: true
})
export class PascalCaseToSpace implements PipeTransform {
  transform(value: string): string {
    if (!value) {
      return value;
    }

    return value.replace(/([a-z])([A-Z][a-z])/g, '$1 $2').replace(/([A-Z])([A-Z][a-z])/g, '$1 $2')
  }
}

// Timestamp to Seconds
@Pipe({
  name: 'timestampToSeconds',
  standalone: true
})
export class TimestampToSeconds implements PipeTransform {
  transform(value: string): number {
    const parts = value.split(':');
    const hours = parseInt(parts[0], 10);
    const minutes = parseInt(parts[1], 10);
    const seconds = parseInt(parts[2], 10);
    return hours * 3600 + minutes * 60 + seconds;
  }
}

// Seconds to Timestamp
@Pipe({
  name: 'secondsToTimestamp',
  standalone: true
})
export class SecondsToTimestamp implements PipeTransform {
  transform(value: any): string {

    const actualValue = (typeof value === 'object' && value !== null && 'value' in value) ? value.value : value

    const hours = Math.floor(actualValue / 3600),
          minutes = Math.floor((actualValue % 3600) / 60),
          seconds = actualValue % 60

    const paddedHours = String(hours).padStart(2, '0'),
          paddedMinutes = String(minutes).padStart(2, '0'),
          paddedSeconds = String(seconds).padStart(2, '0')

    return `${paddedHours}:${paddedMinutes}:${paddedSeconds}`
  }
}

// Truncated, seconds to Hours
@Pipe({
  name: 'secondsToHours',
  standalone: true
})
export class SecondsToHours implements PipeTransform {
  transform(value: number): string {
    const hours = Math.round(value / 3600)

    return hours.toString()
  }
}

// Milliseconds to Timestamp
@Pipe({
  name: 'millisecondsToTimestamp',
  standalone: true
})
export class MillisecondsToTimestamp implements PipeTransform {
  transform(value: any): string {

    const actualValue = (typeof value === 'object' && value !== null && 'value' in value) ? value.value : value

    const hours = Math.floor((actualValue / (1000 * 60 * 60)) % 24),
          minutes = Math.floor((actualValue / (1000 * 60)) % 60),
          seconds = Math.floor((actualValue / 1000) % 60)

    const paddedHours = String(hours).padStart(2, '0'),
          paddedMinutes = String(minutes).padStart(2, '0'),
          paddedSeconds = String(seconds).padStart(2, '0')

    return `${paddedHours}:${paddedMinutes}:${paddedSeconds}`
  }
}

// Truncated, milliseconds to Minutes
@Pipe({
  name: 'millisecondsToDisplay',
  standalone: true
})
export class MillisecondsToDisplay implements PipeTransform {
  transform(actualValue: number) {
    const hours = Math.floor((actualValue / (1000 * 60 * 60)) % 24),
          minutes = Math.floor((actualValue / (1000 * 60)) % 60),
          seconds = Math.floor((actualValue / 1000) % 60)

    if(hours > 0) {
      const remainder = (minutes / 60 * 100).toFixed(0)
      return {value: `${hours.toString()}.${remainder.toString()}`, unit: 'hours'}
    } else {
      const remainder = (seconds / 60 * 100).toFixed(0)
      return {value: `${minutes.toString()}.${remainder.toString()}`, unit: 'minutes'}
    }
  }
}

// Round to thousand (but also million)
@Pipe({
  name: 'roundToThousand',
  standalone: true
})
export class RoundToThousand implements PipeTransform {
  transform(value: number): string {
    if (value > 999999) {
      const rounded = Math.round(value / 1000000)
      return `${rounded}m`
    } else if (value > 999) {
      const rounded = Math.round(value / 1000)
      return `${rounded}k`
    } else {
      return value.toString()
    }
  }
}

// Convert YYYY-MM-DD to MM.DD.YYYY
@Pipe({
  name: 'convertDateString',
  standalone: true
})
export class ConvertDateString implements PipeTransform {
  transform(date: string): string {
    const parts = date.split('-')
    return `${parts[1]}.${parts[2]}.${parts[0]}`
  }
}