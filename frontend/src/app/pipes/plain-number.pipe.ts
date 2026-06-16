import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'plainNumber',
  standalone: true
})
export class PlainNumberPipe implements PipeTransform {
  transform(value: number | string | null | undefined, digits = 2): string {
    if (value === null || value === undefined || value === '') {
      return '';
    }
    const num = Number(value);
    if (Number.isNaN(num)) {
      return String(value);
    }
    return num.toFixed(Number(digits));
  }
}
