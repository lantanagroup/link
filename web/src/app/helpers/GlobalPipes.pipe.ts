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

    const hours = Math.floor(actualValue / 3600);
    const minutes = Math.floor((actualValue % 3600) / 60);
    const seconds = actualValue % 60;

    const paddedHours = String(hours).padStart(2, '0');
    const paddedMinutes = String(minutes).padStart(2, '0');
    const paddedSeconds = String(seconds).padStart(2, '0');

    return `${paddedHours}:${paddedMinutes}:${paddedSeconds}`;
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