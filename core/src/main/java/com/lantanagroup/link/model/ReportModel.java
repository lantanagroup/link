package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter @Setter
public class ReportModel {

    private String version;
    private String status;
    private Date date;
    private List<ReportMeasure> reportMeasureList = new ArrayList<>();

    @Getter
    @Setter
    public static class ReportMeasure {
        private String identifier;
        private String bundleId;
        private Measure measure;
        private MeasureReport measureReport;
    }
}
