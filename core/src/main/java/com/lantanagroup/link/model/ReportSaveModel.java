package com.lantanagroup.link.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lantanagroup.link.serialize.ReportSaveModelDeserializer;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

@Getter @Setter
public class ReportSaveModel {
    @JsonDeserialize(using = ReportSaveModelDeserializer.class)
    MeasureReport measureReport;

    @JsonDeserialize(using = ReportSaveModelDeserializer.class)
    QuestionnaireResponse questionnaireResponse;
}
