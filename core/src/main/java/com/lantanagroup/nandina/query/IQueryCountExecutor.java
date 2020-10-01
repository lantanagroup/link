package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.NandinaConfig;

import java.util.Map;

public interface IQueryCountExecutor {
	Integer execute();

	Map<String, String> getCriteria();
	void setCriteria(Map<String, String> criteria);

	Map<String, Object> getContextData();
	void setContextData(Map<String, Object> contextData);

	NandinaConfig getProperties();
	void setProperties(NandinaConfig properties);

	IGenericClient getFhirClient();
	void setFhirClient(IGenericClient client);
}
