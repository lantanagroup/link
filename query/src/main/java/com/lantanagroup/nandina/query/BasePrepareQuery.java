package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;

import java.util.Map;

public abstract class BasePrepareQuery implements IPrepareQuery {
    protected JsonProperties properties;
    protected IGenericClient fhirClient;
    protected Map<String, String> criteria;
    protected Map<String, Object> contextData;

    public JsonProperties getProperties() {
        return this.properties;
    }

    public void setProperties(JsonProperties properties) {
        this.properties = properties;
    }

    public IGenericClient getFhirClient() {
        return this.fhirClient;
    }

    public void setFhirClient(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public Map<String, String> getCriteria() {
        return this.criteria;
    }

    public void setCriteria(Map<String, String> criteria) {
        this.criteria = criteria;
    }

    public Map<String, Object> getContextData() {
        return this.contextData;
    }

    public void setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
    }

    public Object getContextData(String key) {
        return this.getContextData().get(key);
    }

    public void addContextData(String key, Object data) {
        this.getContextData().put(key, data);
    }
}
