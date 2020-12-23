import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.nandina.query.PatientData;
import com.lantanagroup.nandina.query.filter.OnsetFilter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class OnsetFilterTest {
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
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("condition-covid-onset-datetimetype.json"));
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
     * True test: test for hospital onset
     */
    @Test
    public void OnsetFilterTestTrue() {
        OnsetFilter onsetFilter = new OnsetFilter(dateCollected);
        Assert.assertTrue(onsetFilter.runFilter(patientData));
    }

    /**
     * False test: test for hospital onset
     */
    @Test
    public void OnsetFilterTestFalse() {
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("condition-covid-not-onset-datetimetype.json"));
        patientData.setConditions(conditions);
        OnsetFilter onsetFilter = new OnsetFilter(dateCollected);
        Assert.assertTrue(onsetFilter.runFilter(patientData));
    }

    /**
     * False test: onset after discharge
     */
    @Test
    public void OnsetFilterAfterDischargeTestFalse() {
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("condition-onset-after-discharge.json"));
        patientData.setConditions(conditions);
        encounters = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("encounters-onset-after-discharge.json"));
        patientData.setEncounters(encounters);
        primaryEncounter = parser.parseResource(Encounter.class, Helper.convertResourceFileToJson("primary-encounter-onset-after-discharge.json"));
        patientData.setPrimaryEncounter(primaryEncounter);
        OnsetFilter onsetFilter = new OnsetFilter(dateCollected);
        Assert.assertFalse(onsetFilter.runFilter(patientData));
    }

    /**
     * False test: onset after discharge
     */
    @Test
    public void testOnsetDuringEncounter() {
        OnsetFilter onsetFilter = new OnsetFilter(dateCollected);

        //assert false for encounter start date is null
        Assert.assertFalse(onsetFilter.onsetDuringEncounter(LocalDate.parse("2020-06-11"), null, LocalDate.parse("2020-06-11")));

        //assert false onset date is not 14 days after encounter start date
        Assert.assertFalse(onsetFilter.onsetDuringEncounter(LocalDate.parse("2020-06-11"), LocalDate.parse("2020-06-15"), LocalDate.parse("2020-06-29")));

        //assert true onset date is 14 days after encounter start date, and encounter end is not null
        Assert.assertTrue(onsetFilter.onsetDuringEncounter(LocalDate.parse("2020-06-26"), LocalDate.parse("2020-06-11"), LocalDate.parse("2020-06-29")));

        //assert true onset date is 14 days after encounter start date, onset date is not after end date
        Assert.assertTrue(onsetFilter.onsetDuringEncounter(LocalDate.parse("2020-06-26"), LocalDate.parse("2020-06-11"), LocalDate.parse("2020-06-29")));

        //assert true onset date is 14 days after encounter start date, onset date is after end date signaling onset after discharge
        Assert.assertFalse(onsetFilter.onsetDuringEncounter(LocalDate.parse("2020-06-30"), LocalDate.parse("2020-06-11"), LocalDate.parse("2020-06-29")));
    }
}
