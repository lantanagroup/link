package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.StopwatchManager;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.query.USCoreQueryParametersResourceConfig;
import com.lantanagroup.link.config.query.USCoreQueryParametersResourceParameterConfig;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Test;

import java.time.Period;
import java.util.HashMap;
import java.util.List;

public class PatientDataTests {
  @Test
  public void getQueryTest_ObservationWithCategoryAndDate() {
    USCoreConfig config = new USCoreConfig();
    config.setQueryParameters(new HashMap<>());
    config.getQueryParameters().put("measure1",
            List.of(new USCoreQueryParametersResourceConfig("Observation",
                    List.of(
                            new USCoreQueryParametersResourceParameterConfig("category", true, List.of("labs", "vital-signs")),
                            new USCoreQueryParametersResourceParameterConfig("date", false, List.of("ge${periodStart}", "le${periodEnd}"))))));

    ReportCriteria criteria = new ReportCriteria(List.of("measure1"), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    PatientData patientData = new PatientData(new StopwatchManager(), new HashMap<>(), null, null, criteria, null, new Patient(), config, List.of("Patient", "Encounter", "MedicationRequest"));

    List<String> queryStrings = patientData.getQuery(List.of("measure1"), "Observation", "patient1");
    Assert.assertEquals(1, queryStrings.size());
    Assert.assertEquals("Observation?patient=Patient/patient1&category=labs,vital-signs&date=ge2022-01-01&date=le2022-01-31", queryStrings.get(0));
  }

  @Test
  public void getQueryTest_ObservationWithLookBackDate() {
    USCoreConfig config = new USCoreConfig();
    config.setLookbackPeriod(Period.ofDays(14));
    config.setQueryParameters(new HashMap<>());
    config.getQueryParameters().put("measure1",
            List.of(new USCoreQueryParametersResourceConfig("Observation",
                    List.of(new USCoreQueryParametersResourceParameterConfig("date", false, List.of("ge${lookBackStart}", "le${periodEnd}"))))));

    ReportCriteria criteria = new ReportCriteria(List.of("measure1"), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    PatientData patientData = new PatientData(new StopwatchManager(), new HashMap<>(), null, null, criteria, null, new Patient(), config, List.of("Patient", "Encounter", "MedicationRequest"));

    List<String> queryStrings = patientData.getQuery(List.of("measure1"), "Observation", "patient1");
    Assert.assertEquals(1, queryStrings.size());
    Assert.assertEquals("Observation?patient=Patient/patient1&date=ge2021-12-18&date=le2022-01-31", queryStrings.get(0));
  }

  @Test
  public void getQueryTest_ObservationWithLookBackDate_NoLookBackConfig() {
    USCoreConfig config = new USCoreConfig();
    config.setQueryParameters(new HashMap<>());
    config.getQueryParameters().put("measure1",
            List.of(new USCoreQueryParametersResourceConfig("Observation",
                    List.of(new USCoreQueryParametersResourceParameterConfig("date", false, List.of("ge${lookBackStart}", "le${periodEnd}"))))));

    ReportCriteria criteria = new ReportCriteria(List.of("measure1"), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    PatientData patientData = new PatientData(new StopwatchManager(), new HashMap<>(), null, null, criteria, null, new Patient(), config, List.of("Patient", "Encounter", "MedicationRequest"));

    List<String> queryStrings = patientData.getQuery(List.of("measure1"), "Observation", "patient1");
    Assert.assertEquals(1, queryStrings.size());
    Assert.assertEquals("Observation?patient=Patient/patient1&date=ge2022-01-01&date=le2022-01-31", queryStrings.get(0));
  }

  @Test
  public void getQueryTest_MedicationRequest() {
    USCoreConfig config = new USCoreConfig();
    config.setQueryParameters(new HashMap<>());
    config.getQueryParameters().put("measure1",
            List.of(new USCoreQueryParametersResourceConfig("Observation",
                    List.of(new USCoreQueryParametersResourceParameterConfig("category", true, List.of("labs"))))));

    ReportCriteria criteria = new ReportCriteria(List.of("measure1"), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    PatientData patientData = new PatientData(new StopwatchManager(), new HashMap<>(), null, null, criteria, null, new Patient(), config, List.of("Patient", "Encounter", "MedicationRequest"));

    List<String> queryStrings = patientData.getQuery(List.of("measure1"), "MedicationRequest", "patient1");
    Assert.assertEquals(1, queryStrings.size());
    Assert.assertEquals("MedicationRequest?patient=Patient/patient1", queryStrings.get(0));
  }

  @Test
  public void getQueryTest_EncounterWithDate() {
    USCoreConfig config = new USCoreConfig();
    config.setQueryParameters(new HashMap<>());
    config.getQueryParameters().put("measure1",
            List.of(new USCoreQueryParametersResourceConfig("Encounter",
                    List.of(new USCoreQueryParametersResourceParameterConfig("date", false, List.of("ge${periodStart}", "le${periodEnd}"))))));

    ReportCriteria criteria = new ReportCriteria(List.of("measure1"), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    PatientData patientData = new PatientData(new StopwatchManager(), new HashMap<>(), null, null, criteria, null, new Patient(), config, List.of("Patient", "Encounter", "MedicationRequest"));

    List<String> queryStrings = patientData.getQuery(List.of("measure1"), "Encounter", "patient1");
    Assert.assertEquals(1, queryStrings.size());
    Assert.assertEquals("Encounter?patient=Patient/patient1&date=ge2022-01-01&date=le2022-01-31", queryStrings.get(0));
  }

  @Test
  public void getQueryTest_Condition_NoConfig() {
    USCoreConfig config = new USCoreConfig();
    config.setQueryParameters(new HashMap<>());
    config.getQueryParameters().put("measure1",
            List.of(new USCoreQueryParametersResourceConfig("Encounter",
                    List.of(new USCoreQueryParametersResourceParameterConfig("date", false, List.of("ge${periodStart}", "le${periodEnd}"))))));

    ReportCriteria criteria = new ReportCriteria(List.of("measure1"), "2022-01-01T00:00:00.000+00:00", "2022-01-31T23:59:59.000+00:00");
    PatientData patientData = new PatientData(new StopwatchManager(), new HashMap<>(), null, null, criteria, null, new Patient(), config, List.of("Patient", "Encounter", "MedicationRequest"));

    List<String> queryStrings = patientData.getQuery(List.of("measure1"), "Condition", "patient1");
    Assert.assertEquals(1, queryStrings.size());
    Assert.assertEquals("Condition?patient=Patient/patient1", queryStrings.get(0));
  }
}
