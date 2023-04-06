package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.TenantService;
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

  private com.lantanagroup.link.db.model.ConceptMap getConceptMap() {
    List<ConceptMap> conceptMaps = new ArrayList<>();
    ConceptMap conceptMap = new ConceptMap();
    // set group
    ConceptMap.ConceptMapGroupComponent conceptMapGroupComponent = new ConceptMap.ConceptMapGroupComponent();
    conceptMapGroupComponent.setSource("http://some-system.com");
    conceptMapGroupComponent.setTarget("http://loinc.org");

    ConceptMap.SourceElementComponent sourceElementComponent = new ConceptMap.SourceElementComponent();
    sourceElementComponent.setCode("some-type");

    // add target to source
    ConceptMap.TargetElementComponent targetElementComponent = new ConceptMap.TargetElementComponent();
    targetElementComponent.setDisplay("Medical Critical Care");
    targetElementComponent.setCode("1027-2");
    sourceElementComponent.getTarget().add(targetElementComponent);

    // add source to group
    List<ConceptMap.SourceElementComponent> sourceElementComponents = new ArrayList<>();
    sourceElementComponents.add(sourceElementComponent);
    conceptMapGroupComponent.setElement(sourceElementComponents);
    conceptMap.addGroup(conceptMapGroupComponent);

    conceptMaps.add(conceptMap);

    return new com.lantanagroup.link.db.model.ConceptMap(conceptMap);
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

    ApplyConceptMaps applyConceptMaps = Mockito.spy(ApplyConceptMaps.class);
    applyConceptMaps.setTenantService(tenantService);

    List<ApplyConceptMapConfig> applyConceptMapConfigsList = new ArrayList<>();
    ApplyConceptMapConfig applyConceptMapConfig = new ApplyConceptMapConfig();
    applyConceptMapConfig.setConceptMapId("local-codes-cerner-test'");
    List<String> fhirPathContexts = new ArrayList<>();
    fhirPathContexts.add("Location.type");
    applyConceptMapConfig.setFhirPathContexts(fhirPathContexts);
    applyConceptMapConfigsList.add(applyConceptMapConfig);
    ApplyConceptMapsConfig applyConceptMapsConfig = new ApplyConceptMapsConfig();
    applyConceptMapsConfig.setConceptMaps(applyConceptMapConfigsList);
    applyConceptMaps.setApplyConceptMapConfig(applyConceptMapsConfig);

    List<PatientOfInterestModel> patientOfInterestModel = new ArrayList<>();
    PatientOfInterestModel model = new PatientOfInterestModel();
    model.setReference("Patient/nhsn-iip-ip102");
    model.setId("10000");
    patientOfInterestModel.add(model);

    context.setPatientsOfInterest(patientOfInterestModel);

    com.lantanagroup.link.db.model.ConceptMap conceptMap = getConceptMap();
    when(tenantService.getConceptMap(any())).thenReturn(conceptMap);

    List<Coding> codes = new ArrayList<>();
    codes.add(getLocation().getType().get(0).getCoding().get(0));
    Mockito.doReturn(codes).when(applyConceptMaps).filterCodingsByPathList(any(), any());

    applyConceptMaps.execute(patientBundle, reportCriteria, context, new ReportContext.MeasureContext());

    Coding changedCoding = codes.get(0);
    Assert.assertEquals(changedCoding.getExtension().size(), 1);
    Assert.assertEquals(changedCoding.getCode(), "1027-2");
  }


  @Test
  public void testFindCodings() {
    TenantService tenantService = mock(TenantService.class);
    ApplyConceptMaps applyConceptMaps = Mockito.spy(ApplyConceptMaps.class);
    applyConceptMaps.setTenantService(tenantService);

    List<String> pathList = new ArrayList<>();
    pathList.add("Location.type");
    when(tenantService.getConceptMap(any())).thenReturn(getConceptMap());
    List<Coding> list = applyConceptMaps.filterCodingsByPathList((DomainResource)getPatienBundle().getEntry().get(0).getResource(), pathList);

    Assert.assertEquals(list.get(0).getCode(), "some-type");
  }

  @Test
  public void testApplyTransformation() {
    TenantService tenantService = mock(TenantService.class);
    ApplyConceptMaps applyConceptMaps = new ApplyConceptMaps();
    applyConceptMaps.setTenantService(tenantService);

    List<String> pathList = new ArrayList<>();
    pathList.add("Location.type");

    Coding coding = getLocation().getType().get(0).getCoding().get(0);
    com.lantanagroup.link.db.model.ConceptMap dbConceptMap = getConceptMap();
    applyConceptMaps.applyTransformation(dbConceptMap.getResource(), List.of(coding));

    Assert.assertEquals(coding.getCode(), "1027-2");
    Assert.assertEquals(coding.getExtension().size(), 1);
  }

}
