package com.lantanagroup.link.csv;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;

import java.io.IOException;

public class MeasureReportToCSVTest {

  @Test
  public void testConvert() throws IOException {
    String xml = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("example-measure-report.xml"));
    FhirContext ctx = FhirContext.forR4();
    MeasureReport measureReport = (MeasureReport) ctx.newXmlParser().parseResource(xml);
    MeasureReportToCSV converter = new MeasureReportToCSV();
    String csv = converter.convert(measureReport);
    System.out.println("test");
  }
}
