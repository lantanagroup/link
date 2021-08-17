package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

@Getter @Setter
public class ReportSaveModel {
    MeasureReport measureReport;
    QuestionnaireResponse questionnaireResponse;
}
