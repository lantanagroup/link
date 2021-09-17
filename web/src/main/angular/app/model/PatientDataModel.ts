import {Condition, Encounter, MedicationRequest, Procedure} from "./fhir";

export class PatientDataModel {
    public conditions: Condition[];
    public encounters: Encounter[];
    public medicationRequests: MedicationRequest[];
    public procedures: Procedure[];
}

