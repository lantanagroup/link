package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.MeasurePopulation;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class ReportAggregatorTests {
  private Bundle getReportDefBundle() {
    Bundle measureDefBundle = new Bundle();
    Measure measure = new Measure();
    measureDefBundle.addEntry().setResource(measure);

    measure.addGroup().addPopulation()
            .setCode(new CodeableConcept(new Coding().setCode(MeasurePopulation.INITIALPOPULATION.toCode())));

    return measureDefBundle;
  }

  private MeasureReport getPatientMeasureReport(String patientId, int populationCount) {
    MeasureReport measureReport = new MeasureReport();
    measureReport.setSubject(new Reference("Patient/" + patientId));
    measureReport.addGroup().addPopulation()
            .setCode(new CodeableConcept(new Coding().setCode(MeasurePopulation.INITIALPOPULATION.toCode())))
            .setCount(populationCount);
    return measureReport;
  }

  private ReportContext.MeasureContext getMeasureContext(String version) {
    ReportContext.MeasureContext context = new ReportContext.MeasureContext();
    context.setMeasure(new Measure());
    context.getMeasure().setUrl("http://test.com/fhir/Measure/SomeMeasure");
    context.getMeasure().setVersion(version);
    context.setReportDefBundle(this.getReportDefBundle());
    context.getPatientReportsByPatientId().put("test-patient1", this.getPatientMeasureReport("test-patient1", 1));
    context.getPatientReportsByPatientId().put("test-patient2", this.getPatientMeasureReport("test-patient2", 2));
    context.getPatientReportsByPatientId().put("test-patient3", this.getPatientMeasureReport("test-patient3", 0));
    return context;
  }

  @Test
  public void aggregationWithoutMeasureVersionTest() throws ParseException {
    ReportContext.MeasureContext context = this.getMeasureContext(null);
    ReportCriteria criteria = new ReportCriteria(
            "test-measure",
            "2023-08-22T00:00:00Z",
            "2023-08-22T23:59:59Z");
    ReportAggregator aggregator = new ReportAggregator();
    MeasureReport aggregate = aggregator.generate(criteria, context);
    Assert.assertEquals("http://test.com/fhir/Measure/SomeMeasure", aggregate.getMeasure());
  }

  @Test
  public void aggregationWithMeasureVersionTest() throws ParseException {
    ReportContext.MeasureContext context = this.getMeasureContext("1.0.1");
    ReportCriteria criteria = new ReportCriteria(
            "test-measure",
            "2023-08-22T00:00:00Z",
            "2023-08-22T23:59:59Z");
    ReportAggregator aggregator = new ReportAggregator();
    MeasureReport aggregate = aggregator.generate(criteria, context);
    Assert.assertEquals("http://test.com/fhir/Measure/SomeMeasure|1.0.1", aggregate.getMeasure());
  }

  @Test
  public void aggregationCountTest() throws ParseException {
    ReportContext.MeasureContext context = this.getMeasureContext(null);
    ReportCriteria criteria = new ReportCriteria(
            "test-measure",
            "2023-08-22T00:00:00Z",
            "2023-08-22T23:59:59Z");
    ReportAggregator aggregator = new ReportAggregator();
    MeasureReport aggregate = aggregator.generate(criteria, context);
    Assert.assertEquals(MeasurePopulation.INITIALPOPULATION.toCode(), aggregate.getGroupFirstRep().getPopulationFirstRep().getCode().getCodingFirstRep().getCode());
    Assert.assertEquals(3, aggregate.getGroupFirstRep().getPopulationFirstRep().getCount());
  }
}
