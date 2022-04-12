import {Condition, Encounter, MedicationRequest, Observation, Procedure, ServiceRequest} from "./fhir";

export class PatientDataModel {
    public conditions: Condition[];
    public encounters: Encounter[];
    public medicationRequests: MedicationRequest[];
    public procedures: Procedure[];
    public observations: Observation[];
    public serviceRequests: ServiceRequest[];
}

