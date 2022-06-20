package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.ainq.saner.converters.csv.CsvToReportConverter;
import com.lantanagroup.link.FhirContextProvider;
import org.apache.http.entity.ContentType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static com.lantanagroup.link.Constants.MeasureReportBundleProfileUrl;

@Component
public class ReportCsvOperationProvider {
  private static final Logger logger = LoggerFactory.getLogger(ReportCsvOperationProvider.class);

  @Autowired
  private IFhirResourceDao<Measure> measureDao;

  @Autowired
  private IFhirResourceDao<Bundle> bundleDao;

  private void validateInput(Binary input) {
    if (input == null) {
      throw new InvalidRequestException("input must not be null");
    }
    if (input.getData() == null) {
      throw new InvalidRequestException("input.data must not be null");
    }
  }

  private void validateDate(DateParam value, String parameterName) {
    if (value == null) {
      throw new InvalidRequestException(String.format("%s must not be null", parameterName));
    }
    if (value.getPrefix() != null) {
      throw new InvalidRequestException(String.format("%s must not have a prefix", parameterName));
    }
    if (value.getPrecision().getCalendarConstant() > TemporalPrecisionEnum.DAY.getCalendarConstant()) {
      throw new InvalidRequestException(String.format("%s must not be more precise than date", parameterName));
    }
  }

  private void validateDates(DateParam periodStart, DateParam periodEnd) {
    validateDate(periodStart, "period-start");
    validateDate(periodEnd, "period-end");
    if (periodEnd.getValue().before(periodStart.getValue())) {
      throw new InvalidRequestException("period-end must not be earlier than period-start");
    }
  }

  private Measure getMeasure(IdType measureId, RequestDetails requestDetails) {
    try {
      logger.debug("Requesting measure from database");
      return measureDao.read(measureId, requestDetails);
    } catch (ResourceNotFoundException | ResourceGoneException restException) {
      logger.debug("Failed with status {}; reading measure from resources", restException.getStatusCode());
      for (Class<?> clazz : List.of(ReportCsvOperationProvider.class, CsvToReportConverter.class)) {
        Module module = clazz.getModule();
        String resourceName = String.format("/%s.xml", measureId.getIdPart());
        try (InputStream stream = module.getResourceAsStream(resourceName)) {
          if (stream == null) {
            continue;
          }
          return FhirContextProvider.getFhirContext().newXmlParser().parseResource(Measure.class, stream);
        } catch (IOException ioException) {
          logger.warn(String.format("Failed to read resource '%s' from %s", resourceName, module), ioException);
        }
      }
      throw restException;
    }
  }

  private Charset getCharset(Binary input) {
    final String EXPECTED_MIME_TYPE = "text/csv";
    if (input.hasContentType()) {
      ContentType contentType;
      try {
        contentType = ContentType.parse(input.getContentType());
      } catch (Exception e) {
        throw new InvalidRequestException("Failed to parse input content type", e);
      }
      if (!contentType.getMimeType().equalsIgnoreCase(EXPECTED_MIME_TYPE)) {
        throw new InvalidRequestException(String.format(
                "Unexpected input MIME type (expected '%s', got '%s')",
                EXPECTED_MIME_TYPE,
                contentType.getMimeType()));
      }
      if (contentType.getCharset() != null) {
        return contentType.getCharset();
      }
    }
    return Charset.defaultCharset();
  }

  private Map<String, String> getHeadersByCode(StringOrListParam map) {
    if (map == null) {
      return null;
    }
    List<StringParam> values = map.getValuesAsQueryTokens();
    Map<String, String> headersByCode = new HashMap<>(values.size());
    for (int index = 0; index < values.size(); index++) {
      String[] components = values.get(index).getValue().split("\\$");
      if (components.length != 2) {
        throw new InvalidRequestException(String.format("Failed to parse map element at index %d", index));
      }
      headersByCode.put(components[0], components[1]);
    }
    return headersByCode;
  }

  private MeasureReport getMeasureReport(
          Measure measure,
          Binary input,
          StringOrListParam map,
          ReferenceParam subject) {
    logger.debug("Converting input data to measure report");
    try (InputStream stream = new ByteArrayInputStream(input.getData());
         Reader reader = new InputStreamReader(stream, getCharset(input))) {
      CsvToReportConverter converter =
              new CsvToReportConverter(measure, new Reference(subject.getValue()), getHeadersByCode(map));
      return converter.convert(reader);
    } catch (IOException e) {
      throw new InvalidRequestException("Failed to convert input data to measure report", e);
    }
  }

  @Operation(type = Measure.class, name = "$report-csv")
  public Bundle execute(
          @IdParam IdType measureId,
          @OperationParam(name = "input", min = 1) Binary input,
          @OperationParam(name = "map") StringOrListParam map,
          @OperationParam(name = "period-end", min = 1) DateParam periodEnd,
          @OperationParam(name = "period-start", min = 1) DateParam periodStart,
          @OperationParam(name = "reporter") ReferenceParam reporter,
          @OperationParam(name = "subject") ReferenceParam subject,
          RequestDetails requestDetails) {
    logger.info("Executing $report-csv");
    validateInput(input);
    validateDates(periodStart, periodEnd);
    Measure measure = getMeasure(measureId, requestDetails);
    MeasureReport measureReport = getMeasureReport(measure, input, map, subject);
    measureReport.setReporter(new Reference(reporter.getValue()));
    measureReport.getPeriod()
            .setStart(periodStart.getValue(), periodStart.getPrecision())
            .setEnd(periodEnd.getValue(), periodEnd.getPrecision());
    Bundle bundle = new Bundle();
    bundle.getMeta().addProfile(MeasureReportBundleProfileUrl);
    bundle.getIdentifier()
            .setSystem("urn:ietf:rfc:3986")
            .setValue(String.format("urn:uuid:%s", UUID.randomUUID()));
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setTimestamp(new Date());
    bundle.addEntry().setResource(measureReport);
    bundleDao.create(bundle, requestDetails);
    return bundle;
  }
}
