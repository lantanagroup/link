package com.lantanagroup.flintlock.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.flintlock.IConfig;

public interface IQueryCountExecutor {
  Integer execute(IConfig config, IGenericClient fhirClient, String reportDate, String overflowLocations);
}
