package com.lantanagroup.nandina.query.fhir.r4.saner;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PrepareQuery extends BasePrepareQuery {
  protected static final Logger logger = LoggerFactory.getLogger(PrepareQuery.class);

  @Override
  public void execute() {
    String reportDate = this.criteria.get("reportDate");
    String overflowLocations = this.criteria.get("overflowLocations");
    Map<String, String> queryCriteria = (Map<String, String>) this.contextData.get("queryCriteria");

    String url = String.format("MeasureReport?_summary=true&date=%s&", Helper.URLEncode(reportDate));

    if (overflowLocations != null && !overflowLocations.isEmpty()) {
      url += String.format("subject=%s&", overflowLocations);
    }

    if (queryCriteria != null) {
      for (String key : queryCriteria.keySet()) {
        url += String.format("%s=%s&", key, queryCriteria.get(key));
      }
    }

    logger.debug("Searching for MeasureReport instances by date");

    Map<String, Resource> results = this.search(url);

    logger.debug(String.format("Found %s MeasureReport entries in summary mode. Retieving full MeasureReprot resources.", results.size()));

    for (String measureReportId : results.keySet()) {
      MeasureReport measureReport = (MeasureReport) results.get(measureReportId);

      if (measureReport.getMeasure() == null || !measureReport.getMeasure().equalsIgnoreCase(Constants.MEASURE_URL)) {
        results.remove(measureReportId);
      }
    }

    logger.debug(String.format("%s MeasureReport entries remain after filtering by 'measure'. Retrieving full MeasureReports...", results.size()));

    results.keySet().parallelStream().forEach(measureReportId -> {
      MeasureReport measureReport = fhirClient
              .read()
              .resource(MeasureReport.class)
              .withId(measureReportId)
              .execute();
      results.put(measureReportId, measureReport);
    });

    this.addContextData("measureReportData", results);
  }
}
