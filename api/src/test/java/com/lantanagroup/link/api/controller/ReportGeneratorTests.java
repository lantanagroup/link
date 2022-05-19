package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ReportGeneratorTests {

  @Autowired
  private ResourceLoader resourceLoader;


  @Ignore
  public void testGenerateAnStore() throws ParseException, IOException {
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    LinkCredentials user = mock(LinkCredentials.class);
    Practitioner practitioner = new Practitioner();
    practitioner.setId("Practitioner/a2927697-0f9d-4240-a551-c16e4b9f5178/_history/1");
    when(user.getPractitioner()).thenReturn(practitioner);

    MeasureReport measureReport = getMeasureReport();
    when(fhirDataProvider.getMeasureReport(anyString(), any(Parameters.class))).thenReturn(measureReport);
    ReportCriteria criteria = new ReportCriteria("https://nhsnlink.org|covid-min", "2021-01-03T00:00:00.000Z", "2021-01-03T23:59:59.000Z");
    ReportContext context = new ReportContext(fhirDataProvider);
    context.setReportId("asfsfsfsfds");
    context.setMeasureId("COVIDMin");
    Bundle bundle = new Bundle();
    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setResource(getMeasure());
    bundle.addEntry(entry);
    context.setReportDefBundle(bundle);
    List patientIds = new ArrayList();
    patientIds.add("73742177-JAN21");
    ReportGenerator generator = new ReportGenerator(context, criteria, new ApiConfig(), user);
    List<MeasureReport> measureReports = generator.generate(criteria, context, patientIds);
    generator.store(measureReports, criteria, context, null);
  }

  @Test
  public void testGenerateAnStoreWithNoIndividualReports() throws ParseException, IOException {
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    LinkCredentials user = mock(LinkCredentials.class);
    Practitioner practitioner = new Practitioner();
    practitioner.setId("Practitioner/a2927697-0f9d-4240-a551-c16e4b9f5178/_history/1");
    when(user.getPractitioner()).thenReturn(practitioner);

    MeasureReport measureReport = getMeasureReport();
    when(fhirDataProvider.getMeasureReport(anyString(), any(Parameters.class))).thenReturn(measureReport);
    ReportCriteria criteria = new ReportCriteria("https://nhsnlink.org|covid-min", "2021-01-03T00:00:00.000Z", "2021-01-03T23:59:59.000Z");
    ReportContext context = new ReportContext(fhirDataProvider);
    context.setReportId("asfsfsfsfds");
    context.setMeasureId("COVIDMin");
    Bundle bundle = new Bundle();
    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setResource(getMeasure());
    bundle.addEntry(entry);
    context.setReportDefBundle(bundle);
    List patientIds = new ArrayList();
    ReportGenerator generator = new ReportGenerator(context, criteria, new ApiConfig(), user);
    // List<MeasureReport> measureReports = generator.generate(criteria, context, patientIds);
    // generator.store(measureReports, criteria, context, null);
  }


  private Measure getMeasure() throws IOException {
    File measure = resourceLoader.getResource("classpath:report-generator-measure.json").getFile();
    String measureJson = FileUtils.readFileToString(measure, StandardCharsets.UTF_8);
    FhirContext ctx = FhirContext.forR4();
    return ctx.newJsonParser().parseResource(Measure.class, measureJson);
  }

  private MeasureReport getMeasureReport() throws IOException {
    File measureReport = resourceLoader.getResource("classpath:report-generator-measure-report.json").getFile();
    String measureReportJson = FileUtils.readFileToString(measureReport, StandardCharsets.UTF_8);
    FhirContext ctx = FhirContext.forR4();
    return ctx.newJsonParser().parseResource(MeasureReport.class, measureReportJson.toString());
  }
}

