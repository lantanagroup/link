package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;

public interface IQueryCountExecutor {
  Integer execute(IConfig config, IGenericClient fhirClient, String reportDate, String overflowLocations);
}
