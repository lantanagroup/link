package com.lantanagroup.flintlock.ecr;

import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ElectronicCaseReportTests {

    private static Patient createTestPatient(String ssn, String first, String last) {
        Patient patient = new Patient();
        Identifier newIdent = patient.addIdentifier();
        newIdent.setValue(ssn);
        newIdent.setSystem("http://hl7.org/fhir/sid/us-ssn");
        HumanName newName = patient.addName();
        newName.addGiven(first);
        newName.setFamily(last);
        return patient;
    }

    private static Observation createTestObservation(String code, String valueCode) {
        Observation obs = new Observation();
        CodeableConcept ccc = new CodeableConcept();
        Coding codeCoding = ccc.addCoding();
        codeCoding.setCode(code);
        obs.setCode(ccc);
        CodeableConcept vcc = new CodeableConcept();
        Coding valueCoding = vcc.addCoding();
        valueCoding.setCode(valueCode);
        obs.setValue(vcc);
        return obs;
    }

    @Test
    public void testNewElectronicCaseReport() {
        Patient testPatient = createTestPatient("432-13-1234", "Bruce", "Wayne");
        ElectronicCaseReport ecr = new ElectronicCaseReport(testPatient);

        List<Observation> socialHistoryObservations = new ArrayList();
        socialHistoryObservations.add(createTestObservation("1234", "4321"));
        ecr.setSocialHistoryEntries(socialHistoryObservations);

        Bundle doc = ecr.compile();

        Assert.assertNotNull(doc);
        Assert.assertEquals(3, doc.getTotal());
        Assert.assertEquals(3, doc.getEntry().size());

        Assert.assertEquals(ResourceType.Composition, doc.getEntry().get(0).getResource().getResourceType());
        Assert.assertNotNull(doc.getEntry().get(0).getResource().getId());

        Assert.assertEquals(ResourceType.Patient, doc.getEntry().get(1).getResource().getResourceType());
        Assert.assertNotNull(doc.getEntry().get(1).getResource().getId());

        Assert.assertEquals(ResourceType.Observation, doc.getEntry().get(2).getResource().getResourceType());
        Assert.assertNotNull(doc.getEntry().get(2).getResource().getId());
    }
}