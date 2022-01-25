package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReportGeneratorTests {


  @Test
  public void testGenerateAnStore() throws ParseException {
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
    MeasureReport masterReport = generator.generateAndStore(criteria, context, patientIds, null);
    Assert.assertEquals(masterReport.getGroup().get(0).getPopulation().size(), 3);
  }

  @Test
  public void testGenerateAnStoreWithNoIndividualReports() throws ParseException {
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
    MeasureReport masterReport = generator.generateAndStore(criteria, context, patientIds, null);
    Assert.assertEquals(masterReport.getGroup().get(0).getPopulation().size(), 3);
  }


  private Measure getMeasure() {

    String measureJson = "{\"resourceType\": \"Measure\",\"id\": \"COVIDMin\",\"identifier\": [ {\"system\": \"https://nhsnlink.org\",\"value\": \"covid-min\"} ],\"name\": \"COVID Minimal\",\"group\": [ {\n" +
            "\"population\": [{\n" +
            "\"id\": \"A0965435-DBF6-491D-96DA-6EB69AB39C20\",\n" +
            "\"code\": {\n" +
            "\"coding\": [{\n" +
            "\"system\": \"http://terminology.hl7.org/CodeSystem/measure-population\",\n" +
            "\"code\": \"initial-population\",\n" +
            "\"display\": \"Initial Population\"\n" +
            "}]}},{\n" +
            "\"id\": \"AF0E8444-5D57-4DFE-9885-BA9E18C5C239\",\n" +
            "\"code\": {\n" +
            "\"coding\": [{\n" +
            "\"system\": \"http://terminology.hl7.org/CodeSystem/measure-population\",\n" +
            "\"code\": \"denominator\",\n" +
            "\"display\": \"Denominator\"\n" +
            "}]}},{\n" +
            "\"id\": \"638C0B32-BDCF-482B-9ADB-FDB9F818CF5B\",\n" +
            "\"code\": {\n" +
            "\"coding\": [{\n" +
            "\"system\": \"http://terminology.hl7.org/CodeSystem/measure-population\",\n" +
            "\"code\": \"numerator\",\n" +
            "\"display\": \"Numerator\"\n" +
            "}]}}]}]}";
    FhirContext ctx = FhirContext.forR4();
    return ctx.newJsonParser().parseResource(Measure.class, measureJson);
  }

  private MeasureReport getMeasureReport() {

    String measureReportJson = "{\"resourceType\": \"MeasureReport\",\n" +
            "\"id\": \"nN7N4170-128172033\",\n" +
            "\"status\": \"complete\",\n" +
            "\"group\": [ {\n" +
            "\"population\": [ {\n" +
            "\"code\": {\n" +
            "\"coding\": [ {\n" +
            "  \"system\": \"http://terminology.hl7.org/CodeSystem/measure-population\",\n" +
            "  \"code\": \"initial-population\",\n" +
            "  \"display\": \"Initial Population\"\n" +
            "} ]\n" +
            "},\n" +
            "\"count\": 1\n" +
            "}, {\n" +
            "\"code\": {\n" +
            "\"coding\": [ {\n" +
            "  \"system\": \"http://terminology.hl7.org/CodeSystem/measure-population\",\n" +
            "  \"code\": \"numerator\",\n" +
            "  \"display\": \"Numerator\"\n" +
            "} ]\n" +
            "},\n" +
            "\"count\": 0\n" +
            "}, {\n" +
            "\"code\": {\n" +
            "\"coding\": [ {\n" +
            "  \"system\": \"http://terminology.hl7.org/CodeSystem/measure-population\",\n" +
            "  \"code\": \"denominator\",\n" +
            "  \"display\": \"Denominator\"\n" +
            "} ]\n" +
            "},\n" +
            "\"count\": 2\n" +
            "} ]\n" +
            "} ]\n" +
            "}";
    FhirContext ctx = FhirContext.forR4();
    return ctx.newJsonParser().parseResource(MeasureReport.class, measureReportJson);
  }

}
