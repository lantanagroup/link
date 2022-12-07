package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.model.CsvEntry;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class PatientIdentifierControllerTests {
  private void mockCreateResource(ICreate create) {
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
    String csvContent = "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,2021-12-12,121,12742537";

    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org");
    });
  }

  @Test
  public void testStoreCSVMissingReportTypeException() throws Exception {
    String csvContent = "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,2021-12-12,121,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "");
    });
  }

  @Test
  public void testStoreCSVInvalidPatientIdentifierException() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000,2021-12-12,2021-12-12,121,12742537" +
            "urn:oid:2.16.840.1.113883.6.1000,303061395,2021-12-12,2021-12-12,121,12742538";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");
    });
  }

  @Test
  public void testStoreCSVWithNoLinesException() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");
    });
  }

  @Test
  public void testStoreCSVWithInvalidDateException() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-14-12,121,2021-14-12,121,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");
    });
  }

  @Test
  public void testStoreCSVWithMissingDateException() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,,,121,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");
    });
  }

  @Test
  public void testStoreCSVWithMissingPatientException() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" + ",2021-14-12,2021-14-12,,12742537";
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Assert.assertThrows(ResponseStatusException.class, () -> {
      PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
      patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");
    });
  }

  @Test
  public void testGetCsvEntries() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,2021-12-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061396,2021-12-12,2021-12-12,121,12742538\n";
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    List<CsvEntry> listCsv = patientIdentifierController.getCsvEntries(csvContent);
    Assert.assertEquals(2, listCsv.size());
  }

  @Test
  public void testCreateOneList() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,2021-12-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061396,2021-12-12,2021-12-12,121,12742538\n";
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    IUntypedQuery<IBaseBundle> listQuery = this.mockListBundle(untypedQuery);
    when(fhirStoreClient.search()).thenReturn(listQuery);

    ICreate create = mock(ICreate.class);
    this.mockCreateResource(create);
    when(fhirStoreClient.create()).thenReturn(create);

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = new FhirDataProvider(fhirStoreClient);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);

    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");

    verify(listQuery, times(1)).forResource(ListResource.class);
  }

  @Test
  public void testCreateTwoLists() throws Exception {
    String csvContent = "PatientIdentifier,Start,End,EncounterID,PatientLogicalID\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061395,2021-12-12,2021-12-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061396,2021-12-12,2021-12-12,121,12742538\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061397,2021-11-12,2021-11-12,121,12742537\n" +
            "urn:oid:2.16.840.1.113883.6.1000|303061398,2021-11-12,2021-11-12,121,12742538\n";

    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    IUntypedQuery<IBaseBundle> listQuery = this.mockListBundle(untypedQuery);
    when(fhirStoreClient.search()).thenReturn(listQuery);

    ICreate create = mock(ICreate.class);
    this.mockCreateResource(create);
    when(fhirStoreClient.create()).thenReturn(create);

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = new FhirDataProvider(fhirStoreClient);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);

    patientIdentifierController.storeCSV(csvContent, "https://nshnlink.org|covid-min");

    verify(listQuery, times(2)).forResource(ListResource.class);
  }

  @Test
  public void testCreateNewListFromXml() throws Exception {
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><List xmlns=\"http://hl7.org/fhir\" xmlns:fhir=\"http://hl7.org/fhir\"><extension url=\"https://www.lantanagroup.com/fhir/StructureDefinition/link-patient-list-applicable-period\" ><valuePeriod><start value= \"2021-11-02T20:00:00.000-04:00\" /><end value= \"2021-11-02T20:00:00.000-04:00\" /></valuePeriod></extension><identifier><system value=\"https://nhsnlink.org\"/><value value=\"covid-min\"/></identifier><status value=\"current\"/><mode value=\"working\"/><entry><item><identifier><system value=\"urn:oid:2.16.840.1.113883.6.1000\"/><value value=\"101062222\"/></identifier></item></entry></List>";

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);
    Bundle bundle = new Bundle();
    bundle.setEntry(new ArrayList<>());
    Bundle repDefBundle = new Bundle();
    repDefBundle.setEntry(new ArrayList<>());
    repDefBundle.getEntry().add(new Bundle.BundleEntryComponent());
    when(fhirDataProvider.searchReportDefinition(anyString(), anyString())).thenReturn(repDefBundle);
    when(fhirDataProvider.findListByIdentifierAndDate("https://nhsnlink.org", "covid-min", "2021-11-02T20:00:00.000Z", "2021-11-02T20:00:00.000Z")).thenReturn(bundle);
    Assert.assertThrows(Exception.class, () -> {
      patientIdentifierController.getPatientIdentifierListXML(xmlContent);
    });
  }

  @Test
  public void testCreateNewListFromJson() throws Exception {
    String jsonContent = "{\"resourceType\":\"List\",\"extension\":[{\"url\":\"https://www.lantanagroup.com/fhir/StructureDefinition/link-patient-list-applicable-period\",\"valuePeriod\":{\"start\":\"2021-11-02T20:00:00.000-04:00\",\"end\":\"2021-11-02T20:00:00.000-04:00\"}}],\"identifier\":[{\"system\":\"https://nhsnlink.org\",\"value\": \"covid-min\"}],\"status\":\"current\",\"mode\":\"working\",\"entry\":[{\"item\":{\"identifier\":{\"system\":\"urn:oid:2.16.840.1.113883.6.1000\",\"value\":\"101062222\"}}}]}";

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);
    Bundle bundle = new Bundle();
    bundle.setEntry(new ArrayList<>());
    Bundle repDefBundle = new Bundle();
    repDefBundle.setEntry(new ArrayList<>());
    repDefBundle.getEntry().add(new Bundle.BundleEntryComponent());

    when(fhirDataProvider.searchReportDefinition(anyString(), anyString())).thenReturn(repDefBundle);
    when(fhirDataProvider.findListByIdentifierAndDate("https://nhsnlink.org", "covid-min", "2021-11-02T20:00:00.000Z", "2021-11-02T20:00:00.000Z")).thenReturn(bundle);
    Assert.assertThrows(Exception.class, () -> {
      patientIdentifierController.getPatientIdentifierListJSON(jsonContent);
    });
  }

  @Test
  public void testUpdateExistingListXml() throws Exception {
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><List xmlns=\"http://hl7.org/fhir\" xmlns:fhir=\"http://hl7.org/fhir\"><extension url=\"https://www.lantanagroup.com/fhir/StructureDefinition/link-patient-list-applicable-period\" ><valuePeriod><start value= \"2021-11-02T20:00:00.000-04:00\" /><end value= \"2021-11-02T20:00:00.000-04:00\" /></valuePeriod></extension><identifier><system value=\"https://nhsnlink.org\"/><value value=\"covid-min\"/></identifier><status value=\"current\"/><mode value=\"working\"/><entry><item><identifier><system value=\"urn:oid:2.16.840.1.113883.6.1000\"/><value value=\"101062222\"/></identifier></item></entry></List>";

    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);
    Bundle bundle = getListBundle("https://nhsnlink.org", "covid-min", "2021-11-02T20:00:00.000-04:00");
    Bundle repDefBundle = new Bundle();
    repDefBundle.setEntry(new ArrayList<>());
    repDefBundle.getEntry().add(new Bundle.BundleEntryComponent());
    when(fhirDataProvider.searchReportDefinition(anyString(), anyString())).thenReturn(repDefBundle);
    when(fhirDataProvider.findListByIdentifierAndDate("https://nhsnlink.org", "covid-min", "2021-11-02T20:00:00.000Z", "2021-11-02T20:00:00.000Z")).thenReturn(bundle);
    Assert.assertThrows(Exception.class, () -> {
      patientIdentifierController.getPatientIdentifierListXML(xmlContent);
    });
  }

  private Bundle getListBundle(String system, String value, String date) {
    Bundle bundle = new Bundle();
    ListResource list = new ListResource();
    ListResource.ListEntryComponent listEntry = new ListResource.ListEntryComponent();
    Identifier patientIdentifier = new Identifier();
    patientIdentifier.setSystemElement(new UriType(system));
    patientIdentifier.setValueElement(new StringType(value));
    Reference reference = new Reference();
    reference.setIdentifier(patientIdentifier);
    listEntry.setItem(reference);
    list.setDateElement(new DateTimeType(date));
    list.addEntry(listEntry);
    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setResource(list);
    bundle.addEntry(entry);
    return bundle;
  }


  @Test
  public void testMissingIdentifierInXml() throws Exception {
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><List xmlns=\"http://hl7.org/fhir\" xmlns:fhir=\"http://hl7.org/fhir\"><status value=\"current\"/><mode value=\"working\"/><date value=\"2021-11-03T00:00:00Z\"/><entry><item><identifier><system value=\"urn:oid:2.16.840.1.113883.6.1000\"/><value value=\"101062222\"/></identifier></item></entry></List>";
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);
    Bundle repDefBundle = new Bundle();
    repDefBundle.setEntry(new ArrayList<>());
    repDefBundle.getEntry().add(new Bundle.BundleEntryComponent());
    when(fhirDataProvider.searchReportDefinition(anyString(), anyString())).thenReturn(repDefBundle);
    Assert.assertThrows(Exception.class, () -> {
      patientIdentifierController.getPatientIdentifierListXML(xmlContent);
    });
  }

  @Test
  public void testMissingDateInXml() throws Exception {
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><List xmlns=\"http://hl7.org/fhir\" xmlns:fhir=\"http://hl7.org/fhir\"><identifier><system value=\"https://nhsnlink.org\"/><value value=\"covid-min\"/></identifier><status value=\"current\"/><mode value=\"working\"/><entry><item><identifier><system value=\"urn:oid:2.16.840.1.113883.6.1000\"/><value value=\"101062222\"/></identifier></item></entry></List>";
    PatientIdentifierController patientIdentifierController = new PatientIdentifierController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    patientIdentifierController.setFhirStoreProvider(fhirDataProvider);
    Bundle repDefBundle = new Bundle();
    repDefBundle.setEntry(new ArrayList<>());
    repDefBundle.getEntry().add(new Bundle.BundleEntryComponent());
    when(fhirDataProvider.searchReportDefinition(anyString(), anyString())).thenReturn(repDefBundle);
    Assert.assertThrows(Exception.class, () -> {
      patientIdentifierController.getPatientIdentifierListXML(xmlContent);
    });
  }
}

