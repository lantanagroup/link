package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.Helper;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.events.EncounterStatusTransformer;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class EncounterStatusTransformerTest {

  @Test
  public void execute() throws ParseException {
    TenantService tenantService = mock(TenantService.class);
    String start = "2022-01-01T00:00:00.000Z";
    String end = "2022-01-31T23:59:59.000Z";
    String reportID = "testID";
    String patientID = "testPatient";
    ReportContext context = new ReportContext();
    EncounterStatusTransformer encounterStatusTransformer = new EncounterStatusTransformer();
    Patient patient = new Patient();
    patient.setId(patientID);
    Encounter encounter1 = new Encounter();
    Encounter encounter2 = new Encounter();
    Period period1 = new Period();
    Period period2 = new Period();
    period1.setStart(Helper.parseFhirDate(start));
    period2.setStart(Helper.parseFhirDate(start));
    period1.setEnd(Helper.parseFhirDate(end));
    encounter1.setPeriod(period1);
    encounter2.setPeriod(period2);
    encounter1.setStatus(Encounter.EncounterStatus.TRIAGED);
    encounter2.setStatus(Encounter.EncounterStatus.TRIAGED);
    Bundle bundle = new Bundle();
    Bundle.BundleEntryComponent bundleEntryComponent1 = new Bundle.BundleEntryComponent();
    Bundle.BundleEntryComponent bundleEntryComponent2 = new Bundle.BundleEntryComponent();
    Bundle.BundleEntryComponent bundleEntryComponent3 = new Bundle.BundleEntryComponent();
    bundleEntryComponent1.setResource(patient);
    bundleEntryComponent2.setResource(encounter1);
    bundleEntryComponent3.setResource(encounter2);
    bundle.getEntry().add(bundleEntryComponent1);
    bundle.getEntry().add(bundleEntryComponent2);
    bundle.getEntry().add(bundleEntryComponent3);
    PatientOfInterestModel patientOfInterest = new PatientOfInterestModel();
    patientOfInterest.setId(patientID);
    List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
    patientsOfInterest.add(patientOfInterest);
    context.setPatientsOfInterest(patientsOfInterest);
    context.setMasterIdentifierValue(reportID);
    String bundleId = ReportIdHelper.getPatientDataBundleId(context.getMasterIdentifierValue(), patientOfInterest.getId());

    encounterStatusTransformer.execute(tenantService, bundle, new ReportCriteria(bundleId, start, end), new ReportContext(), new ReportContext.MeasureContext());

    Encounter encounterTest1 = (Encounter)bundle.getEntry().get(1).getResource();
    Encounter encounterTest2 = (Encounter)bundle.getEntry().get(2).getResource();
    //Rewriting these tests to check that the transformed values persist rather than checking that they DON'T persist
    Assert.assertEquals(encounterTest1.getStatus(), Encounter.EncounterStatus.FINISHED);
    Assert.assertTrue(encounterTest1.getStatusElement().hasExtension());
    Assert.assertEquals(encounterTest2.getStatus(), Encounter.EncounterStatus.TRIAGED);
    Assert.assertFalse(encounterTest2.getStatusElement().hasExtension());
  }
}
