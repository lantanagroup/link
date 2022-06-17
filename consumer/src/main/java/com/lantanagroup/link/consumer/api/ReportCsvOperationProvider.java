package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import com.ainq.saner.converters.csv.CsvToReportConverter;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Date;
import java.util.UUID;

import static com.lantanagroup.link.Constants.MeasureReportBundleProfileUrl;

@Component
public class ReportCsvOperationProvider {
  private static final Logger logger = LoggerFactory.getLogger(ReportCsvOperationProvider.class);

  @Autowired
  private IFhirResourceDao<Measure> measureDao;

  @Operation(type = Measure.class, name = "$report-csv")
  public Bundle execute(
          @IdParam IdType measureId,
          @OperationParam(name = "input", min = 1) Binary input,
          @OperationParam(name = "map") StringOrListParam map,
          @OperationParam(name = "period-end", min = 1) DateParam periodEnd,
          @OperationParam(name = "period-start", min = 1) DateParam periodStart,
          @OperationParam(name = "reporter") ReferenceParam reporter,
          @OperationParam(name = "subject") ReferenceParam subject) throws IOException {
    // TODO: Add logging
    // TODO: Validate arguments
    // TODO: Add error/null checking
    Measure measure = measureDao.read(measureId);
    MeasureReport measureReport;
    try (InputStream stream = new ByteArrayInputStream(input.getData());
         Reader reader = new InputStreamReader(stream)) {
      // TODO: Pass subject and map to converter
      CsvToReportConverter converter = new CsvToReportConverter(measure, null, null);
      measureReport = converter.convert(reader);
    }
    // TODO: Set measure report elements
    Bundle bundle = new Bundle();
    bundle.getMeta().addProfile(MeasureReportBundleProfileUrl);
    bundle.getIdentifier()
            .setSystem("urn:ietf:rfc:3986")
            .setValue(String.format("urn:uuid:%s", UUID.randomUUID()));
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setTimestamp(new Date());
    bundle.addEntry().setResource(measureReport);
    return bundle;
  }
}
