package com.lantanagroup.link.thsa;

import com.ainq.saner.converters.csv.CsvToReportConverter;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IDataProcessor;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.thsa.THSAConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class GenericCSVProcessor implements IDataProcessor {

  @Autowired
  private THSAConfig thsaConfig;

  @Override
  public void process(byte[] dataContent, FhirDataProvider fhirDataProvider) {

    MeasureReport measureReport = new MeasureReport();

    InputStream measureStream = getClass().getClassLoader().getResourceAsStream("THSAMasterAggregate.xml");
    Measure measure = FhirContextProvider.getFhirContext().newXmlParser().parseResource(Measure.class, measureStream);

    InputStream contentStream = new ByteArrayInputStream(dataContent);
    Reader reader = new InputStreamReader(contentStream);

    CsvToReportConverter converter = new CsvToReportConverter(measure, null, null);
    try {
      measureReport = converter.convert(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }

    measureReport.setId(this.thsaConfig.getDataMeasureReportId());

    // Store report
    Bundle updateBundle = new Bundle();
    updateBundle.setType(Bundle.BundleType.TRANSACTION);
    updateBundle.addEntry()
            .setResource(measureReport)
            .setRequest(new Bundle.BundleEntryRequestComponent()
                    .setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl("MeasureReport/" + this.thsaConfig.getDataMeasureReportId()));
    Bundle response = fhirDataProvider.transaction(updateBundle);
  }
}
