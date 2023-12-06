import { Component, Input, OnInit, ElementRef, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { catchError, map, tap } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';

@Component({
  selector: 'app-icon',
  inputs: ['class', 'filePath', 'altText'],
  standalone: true,
  imports: [CommonModule],
  templateUrl: './icon.component.html',
  styleUrls: ['./icon.component.scss']
})
export class IconComponent {
  @Input() class?: string = '' ;
  @Input() filePath?: string;
  @Input() altText?: string = '';

  isSvg: boolean = false
  svgContent: string | undefined | null;
  dynamicClass: string[] = ['icon'];

  constructor(private http: HttpClient, private elementRef: ElementRef, private renderer: Renderer2) {}

  ngOnInit(): void {

    if(this.filePath) {
      this.isSvg = this.filePath.toLowerCase().endsWith('.svg');

      if(this.isSvg) {
        this.http.get(this.filePath, {responseType: 'text'}).pipe(
          map((data: string) => this.svgContent = data),
          catchError((error: any) => {
            console.error('Failed to load SVG:', error);
            this.svgContent = null;
            return EMPTY as Observable<never>;
          }),
          tap(() => this.renderSvg())
        ).subscribe()
      }
    } else {
      this.svgContent = null;
    }

    this.dynamicClass = ['icon', ...(this.class ?? '').split(' ').filter(Boolean)];

  }

  private renderSvg(): void {
    if (this.isSvg) {
      if (this.svgContent === undefined || this.svgContent === null) {
        console.error('SVG content is undefined or null.');
      } else {

        // Clear existing content
        this.renderer.setProperty(this.elementRef.nativeElement, 'innerHTML', '');
  
        // Create an SVG element and append it to the host element
        const svgElement = new DOMParser().parseFromString(this.svgContent, 'image/svg+xml').documentElement;
        this.renderer.appendChild(this.elementRef.nativeElement, svgElement);
      }
    }
  }
}
