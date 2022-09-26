package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.Date;

@Getter @Setter
public class ReportModel {
    String identifier;
    String bundleId;
    String version;
    String status;
    Date date;
    Measure measure;
    MeasureReport measureReport;
}
