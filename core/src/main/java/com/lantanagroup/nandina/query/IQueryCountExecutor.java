package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;

import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

public interface IQueryCountExecutor {
	public Integer execute(String reportDate, String overflowLocations);

}
