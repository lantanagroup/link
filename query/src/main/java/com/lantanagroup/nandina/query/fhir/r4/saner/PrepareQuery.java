package com.lantanagroup.nandina.query.fhir.r4.saner;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import org.hl7.fhir.r4.model.Resource;

import java.util.Map;

public class PrepareQuery extends BasePrepareQuery {
  @Override
  public void execute() {
    String reportDate = this.criteria.get("reportDate");
    String overflowLocations = this.criteria.get("overflowLocations");
    Map<String, String> queryCriteria = (Map<String, String>) this.contextData.get("queryCriteria");

    String url = String.format("MeasureReport?measure=%s&date=%s&",
            Helper.URLEncode(Constants.MEASURE_URL),
            Helper.URLEncode(reportDate));

    if (overflowLocations != null && !overflowLocations.isEmpty()) {
      url += String.format("subject=%s&", overflowLocations);
    }

    if (queryCriteria != null) {
      for (String key : queryCriteria.keySet()) {
        url += String.format("%s=%s&", key, queryCriteria.get(key));
      }
    }

    Map<String, Resource> results = this.search(url);
    this.addContextData("measureReportData", results);
  }
}
