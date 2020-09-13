package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;

import java.util.Map;

public interface IPrepareQuery {
  void execute() throws Exception;

  Map<String, String> getCriteria();
  void setCriteria(Map<String, String> criteria);

  Map<String, Object> getContextData();
  void setContextData(Map<String, Object> contextData);

  JsonProperties getProperties();
  void setProperties(JsonProperties properties);

  IGenericClient getFhirClient();
  void setFhirClient(IGenericClient client);
}
