import {IBundle, IComposition, ICondition, IPatient} from "./fhir";
import {conditionallyCreateMapObjectLiteral} from "@angular/compiler/src/render3/view/util";

export class ReportTableInfo {
  private readonly composition: IComposition;

  doc: IBundle;
  name?: string;
  conditions?: string[];

  constructor(doc: IBundle) {
    this.doc = doc;

    const compEntry = (doc.entry || []).find(e => e.resource && e.resource.resourceType === 'Composition');

    if (compEntry) {
      this.composition = compEntry.resource;
      this.populatePatient();
      this.populateConditions();
    }
  }

  private populateConditions() {
    const conditionsSection = (this.composition.section || []).find(s => s.code && s.code.coding && s.code.coding.length > 0 && s.code.coding[0].code === '11450-4');

    if (conditionsSection) {
      this.conditions = (conditionsSection.entry || []).map(e => {
        const found = (this.doc.entry || []).find(next => next.fullUrl === e.reference);
        const condition: ICondition = found ? found.resource : null;

        if (condition && condition.code && condition.code.coding && condition.code.coding.length > 0) {
          const coding = condition.code.coding[0];
          return coding.display || coding.code;
        } else {
          return 'Reference not found';
        }
      });
    }
  }

  private populatePatient() {
    if (this.composition.subject && this.composition.subject.reference) {
      const patientEntry = (this.doc.entry || []).find(e => e.fullUrl === this.composition.subject.reference);
      const patient: IPatient = patientEntry ? patientEntry.resource : null;

      if (patient && patient.name && patient.name.length > 0) {
        if (patient.name[0].family && patient.name[0].given && patient.name[0].given.length > 0) {
          this.name = patient.name[0].family + ', ' + patient.name[0].given.join(' ');
        } else if (patient.name[0].family) {
          this.name = patient.name[0].family;
        } else if (patient.name[0].given && patient.name[0].given.length > 0) {
          this.name = patient.name[0].given.join(' ');
        }
      }
    }
  }
}
