package com.lantanagroup.link;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class GenericXLSProcessor implements IDataProcessor {
  private static final Logger logger = LoggerFactory.getLogger(GenericXLSProcessor.class);

  @Override
  public void process(byte[] dataContent, FhirDataProvider fhirDataProvider) {
    MeasureReport measureReport = new MeasureReport();

    Measure measure = new Measure();
    try(InputStream measureStream = getClass().getClassLoader().getResourceAsStream("ParklandMasterAggregate.xml")) {
      measure = FhirContextProvider.getFhirContext().newXmlParser().parseResource(Measure.class, measureStream);
    } catch(IOException ex){
      logger.error("Error retrieving measure in Parkland data processor: " + ex.getMessage());
    }
  }
}
