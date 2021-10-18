import {CodeableConcept} from "./fhir";

export class ExcludedPatientModel {
    patientId: string;
    reason: CodeableConcept;
}
