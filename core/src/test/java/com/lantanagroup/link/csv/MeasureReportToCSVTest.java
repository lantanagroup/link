package com.lantanagroup.link.csv;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirContextProvider;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

public class MeasureReportToCSVTest {

  @Test
  public void testConvert() throws IOException {
    String xml = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("example-measure-report.xml"), Charset.defaultCharset());
    FhirContext ctx = FhirContextProvider.getFhirContext();
    MeasureReport measureReport = (MeasureReport) ctx.newXmlParser().parseResource(xml);
    MeasureReportToCSV converter = new MeasureReportToCSV();
    String csv = converter.convert(measureReport);
    System.out.println("test");
  }
}
