package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.QueryReport;

import java.util.Map;

public abstract class BaseFormQuery implements IFormQuery {
  protected Map<String, String> criteria;
  protected Map<String, Object> contextData;
  protected JsonProperties properties;
  protected IGenericClient fhirClient;

  @Override
  public Map<String, String> getCriteria() {
    return this.criteria;
  }

  @Override
  public void setCriteria(Map<String, String> criteria) {
    this.criteria = criteria;
  }

  @Override
  public Map<String, Object> getContextData() {
    return this.contextData;
  }

  @Override
  public void setContextData(Map<String, Object> contextData) {
    this.contextData = contextData;
  }

  @Override
  public JsonProperties getProperties() {
    return this.properties;
  }

  @Override
  public void setProperties(JsonProperties properties) {
    this.properties = properties;
  }

  @Override
  public IGenericClient getFhirClient() {
    return this.fhirClient;
  }

  @Override
  public void setFhirClient(IGenericClient client) {
    this.fhirClient = client;
  }

  protected Object getContextData(String key) {
    if (this.getContextData() == null) {
      throw new IllegalArgumentException("contextData");
    }
    return this.getContextData().get(key);
  }

  protected void addContextData(String key, Object value) {
    if (this.getContextData() == null) {
      throw new IllegalArgumentException("contextData");
    }
    this.getContextData().put(key, value);
  }

  protected void setAnswer(String questionId, Object value) {
    if (!this.getContextData().containsKey("report")) {
      throw new IllegalArgumentException("report");
    }

    QueryReport report = (QueryReport) this.getContextData().get("report");
    report.setAnswer(questionId, value);
  }
}
