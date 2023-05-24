package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.events.ApplyConceptMaps;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApplyConceptMapsTest {

  private ConceptMap getConceptMap() {
    org.hl7.fhir.r4.model.ConceptMap conceptMap = new org.hl7.fhir.r4.model.ConceptMap();
    // set group
    org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent conceptMapGroupComponent = new org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent();
    conceptMapGroupComponent.setSource("http://some-system.com");
    conceptMapGroupComponent.setTarget("http://loinc.org");

    org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent sourceElementComponent = new org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent();
    sourceElementComponent.setCode("some-type");

    // add target to source
    org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent targetElementComponent = new org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent();
    targetElementComponent.setDisplay("Medical Critical Care");
    targetElementComponent.setCode("1027-2");
    sourceElementComponent.getTarget().add(targetElementComponent);

    // add source to group
    List<org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent> sourceElementComponents = new ArrayList<>();
    sourceElementComponents.add(sourceElementComponent);
    conceptMapGroupComponent.setElement(sourceElementComponents);
    conceptMap.addGroup(conceptMapGroupComponent);

    return conceptMap;
  }


  private Bundle getPatienBundle() {
    Bundle patientBundle = new Bundle();
    Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
    Location location = getLocation();
    bundleEntryComponent.setResource(location);
    patientBundle.getEntry().add(bundleEntryComponent);
    return patientBundle;
  }

  private Location getLocation() {
    Location location = new Location();
    CodeableConcept codeableConcept = new CodeableConcept();
    Coding locationCoding = new Coding();
    locationCoding.setCode("some-type");
    locationCoding.setSystem("http://some-system.com");
    codeableConcept.addCoding(locationCoding);
    List<CodeableConcept> list = new ArrayList<>();
    list.add(codeableConcept);
    location.setType(list);
    return location;
  }

  @Test
  public void testExecute() {
    Bundle patientBundle = getPatienBundle();
    ReportCriteria reportCriteria = mock(ReportCriteria.class);
    TenantService tenantService = mock(TenantService.class);

    ReportContext context = new ReportContext();
    context.setMasterIdentifierValue("test");

    Tenant tenant = new Tenant();
    tenant.setId("test");
    when(tenantService.getConfig()).thenReturn(tenant);

    ApplyConceptMaps applyConceptMaps = Mockito.spy(ApplyConceptMaps.class);

    com.lantanagroup.link.db.model.ConceptMap dbConceptMap = new com.lantanagroup.link.db.model.ConceptMap();
    dbConceptMap.setId("local-codes-cerner-test'");
    dbConceptMap.setContexts(List.of("Location.type"));
    dbConceptMap.setConceptMap(getConceptMap());
    when(tenantService.getAllConceptMaps()).thenReturn(List.of(dbConceptMap));

    List<PatientOfInterestModel> patientOfInterestModel = new ArrayList<>();
    PatientOfInterestModel model = new PatientOfInterestModel();
    model.setReference("Patient/nhsn-iip-ip102");
    model.setId("10000");
    patientOfInterestModel.add(model);

    context.setPatientsOfInterest(patientOfInterestModel);

    List<Coding> codes = new ArrayList<>();
    codes.add(getLocation().getType().get(0).getCoding().get(0));
    Mockito.doReturn(codes).when(applyConceptMaps).filterCodingsByPathList(any(), any());

    applyConceptMaps.execute(tenantService, patientBundle);

    Coding changedCoding = codes.get(0);
    Assert.assertEquals(changedCoding.getExtension().size(), 1);
    Assert.assertEquals(changedCoding.getCode(), "1027-2");
  }


  @Test
  public void testFindCodings() {
    TenantService tenantService = mock(TenantService.class);
    ApplyConceptMaps applyConceptMaps = Mockito.spy(ApplyConceptMaps.class);

    com.lantanagroup.link.db.model.ConceptMap dbConceptMap = new com.lantanagroup.link.db.model.ConceptMap();
    dbConceptMap.setId("local-codes-cerner-test'");
    dbConceptMap.setContexts(List.of("Location.type"));
    dbConceptMap.setConceptMap(getConceptMap());
    when(tenantService.getAllConceptMaps()).thenReturn(List.of(dbConceptMap));

    List<Coding> list = applyConceptMaps.filterCodingsByPathList((DomainResource) getPatienBundle().getEntry().get(0).getResource(), dbConceptMap.getContexts());

    Assert.assertEquals(list.get(0).getCode(), "some-type");
  }

  @Test
  public void testApplyTransformation() {
    TenantService tenantService = mock(TenantService.class);
    ApplyConceptMaps applyConceptMaps = new ApplyConceptMaps();

    Coding coding = getLocation().getType().get(0).getCoding().get(0);

    com.lantanagroup.link.db.model.ConceptMap dbConceptMap = new com.lantanagroup.link.db.model.ConceptMap();
    dbConceptMap.setId("local-codes-cerner-test'");
    dbConceptMap.setContexts(List.of("Location.type"));
    dbConceptMap.setConceptMap(getConceptMap());
    when(tenantService.getAllConceptMaps()).thenReturn(List.of(dbConceptMap));

    applyConceptMaps.applyTransformation(dbConceptMap.getConceptMap(), List.of(coding));

    Assert.assertEquals(coding.getCode(), "1027-2");
    Assert.assertEquals(coding.getExtension().size(), 1);
  }

}
