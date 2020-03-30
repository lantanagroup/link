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

    // TODO Create a test PractitionerRole instead
    private static Practitioner createTestPractitioner(String prn, String first, String last) {
        Practitioner practitioner = new Practitioner();
        Identifier identifier = practitioner.addIdentifier();
        identifier.setValue(prn);
        HumanName name = practitioner.addName();
        name.addGiven(first);
        name.setFamily(last);
        return practitioner;
    }

    @Test
    public void testNewElectronicCaseReport() {
        Patient testPatient = createTestPatient("432-13-1234", "Bruce", "Wayne");
        ElectronicCaseReport ecr = new ElectronicCaseReport(null, testPatient, null, null);

        List<Observation> socialHistoryObservations = new ArrayList();
        socialHistoryObservations.add(createTestObservation("1234", "4321"));
        ecr.setSocialHistoryEntries(socialHistoryObservations);

        Bundle doc = ecr.compile();

        Assert.assertNotNull(doc);
        Assert.assertEquals(4, doc.getEntry().size());
    }

    @Test
    public void testNewElectronicCaseReportWithAuthor() {
        Patient testPatient = createTestPatient("432-13-1234", "Bruce", "Wayne");
        Practitioner testPractitioner = createTestPractitioner("some-prn-number", "Peter", "Clarke");
        ElectronicCaseReport ecr = new ElectronicCaseReport(null, testPatient, null, null);

        List<Observation> socialHistoryObservations = new ArrayList();
        socialHistoryObservations.add(createTestObservation("1234", "4321"));
        ecr.setSocialHistoryEntries(socialHistoryObservations);

        Bundle doc = ecr.compile();

        Assert.assertNotNull(doc);
        Assert.assertEquals(4, doc.getEntry().size());
    }
}