import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.HospitalizedEncounterFilter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class HospitalizedEncounterFilterTest {
    private FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();
    private PatientData patientData = null;
    private Patient patient;
    private Encounter primaryEncounter;
    private Bundle encounters;
    private Bundle conditions;
    private Bundle meds;
    private Bundle labResults;
    private Bundle allergies;
    private Bundle procedures;
    private Date dateCollected;
    private CodeableConcept primaryDx;

    @Before
    public void setup() {
        patientData = new PatientData();
        patient = parser.parseResource(Patient.class, Helper.convertResourceFileToJson("patient.json"));
        primaryEncounter = parser.parseResource(Encounter.class, Helper.convertResourceFileToJson("primaryEncounter.json"));
        encounters = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("encounters.json"));
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("conditions-covid1.json"));
        meds = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("meds.json"));
        labResults = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("labResults.json"));
        allergies = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("allergies.json"));
        procedures = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("procedures.json"));
        LocalDate localDate = LocalDate.parse("2020-06-11");
        dateCollected = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        patientData.setPatient(patient);
        patientData.setPrimaryEncounter(primaryEncounter);
        patientData.setEncounters(encounters);
        patientData.setConditions(conditions);
        patientData.setMeds(meds);
        patientData.setLabResults(labResults);
        patientData.setAllergies(allergies);
        patientData.setProcedures(procedures);
        patientData.setDateCollected(dateCollected);
    }

    /**
     * True Test: Test for hospitalized encounter
     */
    @Test
    public void hospitalizedEncounterFilterTrue() {
        encounters = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("hospitalizedEncounter.json"));
        patientData.setEncounters(encounters);
        HospitalizedEncounterFilter hospitalizedEncounterFilter = new HospitalizedEncounterFilter();
        Assert.assertTrue(hospitalizedEncounterFilter.runFilter(patientData));
    }

    /**
     * False Test: Test for a non hospitalized encounter
     */
    @Test
    public void hospitalizedEncounterFilterFalse() {
        encounters = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("notHospitalizedEncounter.json"));
        patientData.setEncounters(encounters);
        HospitalizedEncounterFilter hospitalizedEncounterFilter = new HospitalizedEncounterFilter();
        Assert.assertFalse(hospitalizedEncounterFilter.runFilter(patientData));
    }
}
