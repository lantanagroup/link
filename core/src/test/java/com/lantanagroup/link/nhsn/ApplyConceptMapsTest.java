package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ApplyConceptMapsTest {

  @Test
  public void execute() {
    String start = "2022-01-01T00:00:00.000Z";
    String end = "2022-01-31T23:59:59.000Z";
    String reportID = "testID";
    String serverBase = "https://dev-fhir.nhsnlink.org/fhir";
    String patientID = "testPatient";
    FhirDataProvider fhirProvider = new FhirDataProvider(serverBase);
    ReportCriteria reportCriteria = new ReportCriteria(reportID, start, end);
    ReportContext context = new ReportContext(fhirProvider);
    ApplyConceptMaps applyConceptMaps = new ApplyConceptMaps();
    List<QueryResponse> patientQueryResponses = new ArrayList<>();
    context.setPatientData(patientQueryResponses);
    Coding coding = new Coding();
    coding.setSystem("urn:oid:2.16.840.1.113883.6.238");
    coding.setCode("2186-5");
    coding.setDisplay("Non Hispanic or Latino");
    Patient patient = new Patient();
    Extension extension = new Extension();
    extension.setUrl(Constants.ConceptMappingExtension);
    extension.setValue(coding.copy());
    patient.addExtension(extension);
    Bundle bundle = new Bundle();
    Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
    bundleEntryComponent.setResource(patient);
    bundle.getEntry().add(bundleEntryComponent);
    QueryResponse patientQueryResponse = new QueryResponse();
    patientQueryResponse.setPatientId(patientID);
    patientQueryResponse.setBundle(bundle);
    patientQueryResponses.add(patientQueryResponse);
    List<ConceptMap> conceptMaps = new ArrayList<>();
    ConceptMap conceptMap = new ConceptMap();
    ConceptMap.ConceptMapGroupComponent conceptMapGroupComponent = new ConceptMap.ConceptMapGroupComponent();
    conceptMapGroupComponent.setSource("urn:oid:2.16.840.1.113883.6.238");
    ConceptMap.SourceElementComponent sourceElementComponent = new ConceptMap.SourceElementComponent();


    sourceElementComponent.setCode(coding.getCode());
    sourceElementComponent.addTarget();

    ConceptMap.TargetElementComponent targetElementComponent = new ConceptMap.TargetElementComponent();
    targetElementComponent.setDisplay(coding.getDisplay());
    targetElementComponent.setCode(coding.getCode());

    sourceElementComponent.getTarget().add(targetElementComponent);

    List<ConceptMap.SourceElementComponent> sourceElementComponents = new ArrayList<>();
    sourceElementComponents.add(sourceElementComponent);
    conceptMapGroupComponent.setElement(sourceElementComponents);
    conceptMapGroupComponent.setTarget(coding.getSystem());

    conceptMap.addGroup(conceptMapGroupComponent);
    conceptMaps.add(conceptMap);
    context.setConceptMaps(conceptMaps);

    List<Coding> codes = ResourceIdChanger.findCodings(context.getPatientData().get(0));
    int codeExtend = codes.get(0).getExtension().size();

    applyConceptMaps.execute(reportCriteria, context, null, null);
    List<Coding> codes2 = ResourceIdChanger.findCodings(context.getPatientData().get(0));
    Assert.assertEquals(codes2.get(0).getExtension().size(), codeExtend + 1);
  }
}
