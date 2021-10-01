package com.lantanagroup.link.model;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.*;
import java.util.Date;

@Getter @Setter
public class ReportModel {
    String identifier;
    String version;
    String status;
    Date date;
    Measure measure;
    MeasureReport measureReport;
}
