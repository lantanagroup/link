import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class CovidFilterTest {
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
        LocalDate localDate = LocalDate.parse("2019-06-11");
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
     * Test for a non covid patient based on conditions not having covid
     */
    @Test
    public void NonCovidConditionFilterTest() {
        CovidFilter covidFilter = new CovidFilter();
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("conditions-nonCovid.json"));
        patientData.setConditions(conditions);
        Assert.assertFalse(covidFilter.runFilter(patientData));
    }

    /**
     * Test for a valid covid condition based on at least one covid condition from list of conditions.
     */
    @Test
    public void SingleCovidConditionFilterTest() {
        CovidFilter covidFilter = new CovidFilter();
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("conditions-covid1.json"));
        patientData.setConditions(conditions);
        Assert.assertTrue(covidFilter.runFilter(patientData));
    }

    /**
     * Test for a valid covid condition based on multiple conditions signaling covid conditions.
     */
    @Test
    public void MultipleCovidConditionsFilterTest() {
        CovidFilter covidFilter = new CovidFilter();
        conditions = parser.parseResource(Bundle.class, Helper.convertResourceFileToJson("conditions-covid-multiple.json"));
        patientData.setConditions(conditions);
        Assert.assertTrue(covidFilter.runFilter(patientData));
    }

}
