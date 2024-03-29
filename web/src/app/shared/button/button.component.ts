import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LinkInterface } from '../interfaces/globals.interface';


@Component({
  selector: 'app-button',
  inputs: ['variant', 'disabled', 'type', 'onClickHandler'],
  standalone: true,
  imports: [CommonModule],
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.scss']
})
export class ButtonComponent {
  @Input() type: 'button' | 'submit' | 'link' = 'button';
  @Input() variant?: 'solid' | 'outline' | 'reverse' | 'outline--reverse' | 'text' = 'solid';
  @Input() disabled?: boolean = false;
  @Input() onClickHandler?: () => void = () => {};
  @Input() link?: LinkInterface; 
  @Input() classes?: string = '';

  getButtonClass = () => {
    const buttonClasses = ['btn']

    if(this.variant) {
      buttonClasses.push('btn--' + this.variant)
    }

    if(this.classes) {
      buttonClasses.push(this.classes)
    }

    return buttonClasses.join(' ')
  }
}
