import {Component, Input, OnInit} from '@angular/core';
import {IMeasureReport, IMeasureReportPopulationComponent} from '../fhir';

@Component({
  selector: 'app-calculated-field',
  templateUrl: './calculated-field.component.html',
  styleUrls: ['./calculated-field.component.css']
})
export class CalculatedFieldComponent implements OnInit {
  private static readonly PopOrigCountUrl = 'https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-pop-orig-count';
  private static readonly PopCountChangedReason = 'https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-pop-count-changed-reason';
  @Input() report: IMeasureReport;
  @Input() groupCode: string;
  @Input() populationCode: string;

  constructor() { }

  get value() {
    const population = this.getPopulation();
    return population ? population.count : null;
  }

  set value(value: number) {
    const population = this.getPopulation();
    if (population) {
      this.ensureOriginalCount(population);
      population.count = value;
    }
  }

  get originalValue() {
    const population = this.getPopulation();
    if (!population) return;
    const origExt = (population.extension || []).find(e => e.url === CalculatedFieldComponent.PopOrigCountUrl);

    if (origExt && origExt.hasOwnProperty('valueInteger')) {
      return origExt.valueInteger;
    }

    return population.count;
  }

  get changedReason(): string {
    const population = this.getPopulation();
    if (!population) return;
    const reasonExt = (population.extension || []).find(e => e.url === CalculatedFieldComponent.PopCountChangedReason);

    if (reasonExt && reasonExt.hasOwnProperty('valueString')) {
      return reasonExt.valueString;
    }
  }

  set changedReason(value: string) {
    const population = this.getPopulation();
    if (!population) return;

    let reasonExt = (population.extension || []).find(e => e.url === CalculatedFieldComponent.PopCountChangedReason);

    if (!value && reasonExt) {
      population.extension.splice(population.extension.indexOf(reasonExt), 1);
      if (population.extension.length === 0) delete population.extension;
    } else if (value && !reasonExt) {
      population.extension = population.extension || [];
      population.extension.push({
        url: CalculatedFieldComponent.PopCountChangedReason,
        valueString: value
      });
    } else if (value && reasonExt) {
      reasonExt.valueString = value;
    }
  }

  ngOnInit(): void {
  }

  private ensureOriginalCount(population: IMeasureReportPopulationComponent) {
    population.extension = population.extension || [];
    let origExt = population.extension.find(e => e.url === CalculatedFieldComponent.PopOrigCountUrl);

    if (!origExt) {
      origExt = {
        url: CalculatedFieldComponent.PopOrigCountUrl,
        valueInteger: population.count
      };
      population.extension.push(origExt);
    }
  }

  private getPopulation() {
    if (!this.report) return;

    const group = (this.report.group || []).find(g => {
      if (this.groupCode) {
        return g.code && g.code.coding && g.code.coding.length > 0 && !!g.code.coding.find(c => c.code === this.groupCode);
      } else {
        return true;
      }
    });

    if (group) {
      return (group.population || []).find(p => {
        return p.code && p.code.coding && p.code.coding.length > 0 && !!p.code.coding.find(c => c.code === this.populationCode);
      });
    }
  }
}
