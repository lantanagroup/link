package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.*;

import java.util.List;

@Getter @Setter
public class PatientDataModel {
    List<Condition> conditions;
    List<MedicationRequest> medicationRequests;
    List<Procedure> procedures;
    List<Encounter> encounters;
    List<Observation> observations;
    List<ServiceRequest> serviceRequests;
}
