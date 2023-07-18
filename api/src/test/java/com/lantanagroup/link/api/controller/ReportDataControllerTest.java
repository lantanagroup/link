package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.datagovernance.DataGovernanceConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportDataControllerTest {

  @Ignore
  public void csvEndPoint() throws Exception {
    // TODO: Remove @Ignore and add unit testing logic
    // TODO: Mock out the ReportDataController so that the config can be set
    ReportDataController reportDataController = new ReportDataController();
    reportDataController.retrieveData("csv content".getBytes(StandardCharsets.UTF_8), "csv");
    reportDataController.retrieveData("csv content".getBytes(StandardCharsets.UTF_8), "csv");
  }

  @Test
  public void expungeDataTest() throws DatatypeConfigurationException {
    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreProvider(mock(FhirDataProvider.class));
    reportDataController.setDataGovernanceConfig(new DataGovernanceConfig());
    reportDataController.getDataGovernanceConfig().setCensusListRetention("PT4H");
    reportDataController.getDataGovernanceConfig().setPatientDataRetention("PT4H");
    reportDataController.getDataGovernanceConfig().setReportRetention("PT4H");
    Date testDate = new Date(-1);

    Bundle censusBundle = new Bundle();
    Bundle patientBundle = new Bundle();
    Bundle reportBundle = new Bundle();

    ListResource census = new ListResource();
    census.setId("census");
    census.getMeta().setLastUpdated(testDate);
    censusBundle.addEntry().setResource(census);

    Bundle patientData1 = new Bundle();
    Bundle patientData2 = new Bundle();
    patientData1.setId("patientData1");
    patientData1.getMeta().addTag(new Coding(Constants.MainSystem, Constants.patientDataTag, "Patient-Data"));
    patientData1.getMeta().setLastUpdated(testDate);
    patientData2.setId("patientData2");
    patientData2.getMeta().setLastUpdated(testDate);
    patientBundle.addEntry().setResource(patientData1);
    patientBundle.addEntry().setResource(patientData2);

    MeasureReport measureReport = new MeasureReport();
    measureReport.setId("measureReport");
    measureReport.getMeta().setLastUpdated(testDate);
    reportBundle.addEntry().setResource(measureReport);

    when(reportDataController.getFhirDataProvider().getAllResourcesByType(ListResource.class)).thenReturn(censusBundle);
    when(reportDataController.getFhirDataProvider().getAllResourcesByType(Bundle.class)).thenReturn(patientBundle);
    when(reportDataController.getFhirDataProvider().getAllResourcesByType(MeasureReport.class)).thenReturn(reportBundle);

    HttpServletRequest  mockedRequest = Mockito.mock(HttpServletRequest.class);
    LinkCredentials user = Mockito.mock(LinkCredentials.class);
    try {
      reportDataController.expungeSpecificData(user, mockedRequest);
    } catch (Exception ignored) {}
  }
}
