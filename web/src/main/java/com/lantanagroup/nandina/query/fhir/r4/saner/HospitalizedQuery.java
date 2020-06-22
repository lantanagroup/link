package com.lantanagroup.nandina.query.fhir.r4.saner;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class HospitalizedQuery extends AbstractSanerQuery implements IQueryCountExecutor {
	public HospitalizedQuery(JsonProperties jsonProperties, IGenericClient fhirClient, HashMap<String, String> criteria) {
		super(jsonProperties, fhirClient, criteria);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Integer execute() {
		if (!this.criteria.containsKey("reportDate")) return null;
		Map<String, Resource> data = this.getData();
		return this.countForPopulation(data, "Encounters", "numC19HospPats");
	}
}
