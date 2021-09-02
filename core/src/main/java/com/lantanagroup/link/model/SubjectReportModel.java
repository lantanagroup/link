package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Procedure;

import java.util.List;

@Getter @Setter
public class SubjectReportModel {
    List<Condition> conditions;
    List<MedicationRequest> medicationRequests;
    List<Procedure> procedures;
    List<Encounter> encounters;
}
