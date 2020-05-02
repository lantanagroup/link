package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;
import org.hl7.fhir.r4.model.Bundle;

public interface IQueryCountExecutor {
	public Integer execute(String reportDate, String overflowLocations);

//	Bundle getData(IConfig config, IGenericClient fhirClient, String reportDate, String overflowLocations);
}
