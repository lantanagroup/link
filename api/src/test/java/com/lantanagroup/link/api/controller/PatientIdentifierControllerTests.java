package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.api.controller.PatientIdentifierController;
import com.lantanagroup.link.model.CsvEntry;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.mockito.Mockito.*;

public class PatientIdentifierControllerTests {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private HttpServletRequest httpServletRequest;

  @Before
  public void setup() {
    httpServletRequest = mock(HttpServletRequest.class);
  }

  private void mockCreateResource(ICreate create) {
    //ListResource resource = mock(ListResource.class);
    ICreateTyped createTyped = mock(ICreateTyped.class);
    MethodOutcome createMethod = mock(MethodOutcome.class);
    when(create.resource(any(ListResource.class))).thenReturn(createTyped);
    when(createTyped.execute()).thenReturn(createMethod);
  }

  private IUntypedQuery<IBaseBundle> mockListBundle(IUntypedQuery<IBaseBundle> untypedQuery, Resource... resResources) {
    IQuery<IBaseBundle> subBundleIntQuery = mock(IQuery.class);
    IQuery<Bundle> subBundleQuery = mock(IQuery.class);

    Bundle responseBundle = new Bundle();

    if (resResources != null) {
      for (Resource resource : resResources) {
        responseBundle.addEntry().setResource(resource);
      }
    }

    when(untypedQuery.forResource(ListResource.class)).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.where(any(ICriterion.class))).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.and(any(ICriterion.class))).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.returnBundle(Bundle.class)).thenReturn(subBundleQuery);
    when(subBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(subBundleQuery);
    when(subBundleQuery.execute()).thenReturn(responseBundle);

    return untypedQuery;
  }

  @Test
  public void testStoreCSVInvalidReportTypeException() throws Exception {
    String csvContent = "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,121,12742537";

    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org", httpServletRequest);
  }

  @Test
  public void testStoreCSVMissingReportTypeException() throws Exception {
    String csvContent = "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,121,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "", httpServletRequest);
  }

  @Test
  public void testStoreCSVInvalidPatientIdentifierException() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000,2021-12-12,121,12742537" +
            "urn:oid:2.16.840.1.113883.6.1000,303061395,2021-12-12,121,12742538";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);
  }

  @Test
  public void testStoreCSVWithNoLinesException() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);
  }

  @Test
  public void testStoreCSVWithInvalidDateException() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-14-12,121,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);
  }

  @Test
  public void testStoreCSVWithMissingDateException() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,,121,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);
  }

  @Test
  public void testStoreCSVWithMissingPatientException() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" + ",2021-14-12,,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    thrown.expect(ResponseStatusException.class);
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);
  }

  @Test
  public void testGetCsvEntries() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061396,2021-12-12,121,12742538\n";
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    List<CsvEntry> listCsv = patientIdentifierController.getCsvEntries(csvContent);
    Assert.assertEquals(2, listCsv.size());
  }

  @Test
  public void testCreateOneList() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061396,2021-12-12,121,12742538\n";
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    IUntypedQuery<IBaseBundle> listQuery = this.mockListBundle(untypedQuery);
    when(fhirStoreClient.search()).thenReturn(listQuery);

    ICreate create = mock(ICreate.class);
    this.mockCreateResource(create);
    when(fhirStoreClient.create()).thenReturn(create);

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.setFhirStoreClient(fhirStoreClient);
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);

    verify(listQuery, times(1)).forResource(ListResource.class);
  }

  @Test
  public void testCreateTwoLists() throws Exception {
    String csvContent = "PatientIdentifier,Date,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061396,2021-12-12,121,12742538\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061397,2021-11-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061398,2021-11-12,121,12742538\n";

    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    IUntypedQuery<IBaseBundle> listQuery = this.mockListBundle(untypedQuery);
    when(fhirStoreClient.search()).thenReturn(listQuery);

    ICreate create = mock(ICreate.class);
    this.mockCreateResource(create);
    when(fhirStoreClient.create()).thenReturn(create);

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    patientIdentifierController.setFhirStoreClient(fhirStoreClient);
    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min", httpServletRequest);

    verify(listQuery, times(2)).forResource(ListResource.class);
  }
}
