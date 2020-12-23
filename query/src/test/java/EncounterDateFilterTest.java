import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.nandina.query.PatientData;
import com.lantanagroup.nandina.query.filter.EncounterDateFilter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class EncounterDateFilterTest {
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
     * True Test: Test for encounter date during test date
     */
    @Test
    public void encounterDateFilterTrue() {
        EncounterDateFilter encounterDateFilter = new EncounterDateFilter(dateCollected);
        Assert.assertTrue(encounterDateFilter.runFilter(patientData));
    }

    /**
     * False Test: Test for encounter date not during test date
     */
    @Test
    public void encounterDateFilterFalse() {
        LocalDate localDate = LocalDate.parse("2020-03-11");
        dateCollected = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        patientData.setDateCollected(dateCollected);
        EncounterDateFilter encounterDateFilter = new EncounterDateFilter(dateCollected);
        Assert.assertFalse(encounterDateFilter.runFilter(patientData));
    }
}
