package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalizedQuery implements IQueryCountExecutor {
  private static final Logger logger = LoggerFactory.getLogger(HospitalizedQuery.class);

  @Override
  public Integer execute(IConfig config, IGenericClient fhirClient, String reportDate, String overflowLocations) {
    if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
      this.logger.error("Covid codes have not been specified in configuration. Cannot execute query.");
      return null;
    }

    try {
      String url = String.format("Patient?_summary=true&_active=true&_has:Condition:patient:code=%s", config.getTerminologyCovidCodes());
      Bundle hospitalizedBundle = fhirClient.search()
              .byUrl(url)
              .returnBundle(Bundle.class)
              .execute();
      return hospitalizedBundle.getTotal();
    } catch (Exception ex) {
      this.logger.error("Could not retrieve hospitalized count: " + ex.getMessage(), ex);
    }

    return null;
  }
}
