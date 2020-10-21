import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.DeathFilter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DeathFilterTest {
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
     * Test for a patient that did not die
     */
    @Test
    public void deathFilterFalse() {
        DeathFilter deathFilter = new DeathFilter(dateCollected);
        Assert.assertFalse(deathFilter.runFilter(patientData));
    }

    /**
     * Test for a patient died but on a different day than specified.
     */
    @Test
    public void deathFilterIncorrectDateFalse() {
        LocalDate localDate = LocalDate.parse("2020-09-11");
        dateCollected = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        patient = parser.parseResource(Patient.class, Helper.convertResourceFileToJson("deceasedPatient.json"));
        patientData.setPatient(patient);
        DeathFilter deathFilter = new DeathFilter(dateCollected);
        Assert.assertFalse(deathFilter.runFilter(patientData));
    }

    /**
     * Test for a patient died but on a different day than specified.
     */
    @Test
    public void deathFilterTrue() {
        patient = parser.parseResource(Patient.class, Helper.convertResourceFileToJson("deceasedPatient.json"));
        patientData.setPatient(patient);
        DeathFilter deathFilter = new DeathFilter(dateCollected);
        Assert.assertTrue(deathFilter.runFilter(patientData));
    }

}
