package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathsQuery extends AbstractQuery implements IQueryCountExecutor {
	
    public DeathsQuery(IConfig config, IGenericClient fhirClient) {
		super(config, fhirClient);
	}

    @Override
    public Integer execute(String reportDate, String overflowLocations) {
    	Map<String,Resource> resMap = this.getData(reportDate, overflowLocations);
        return this.getCount(resMap);
    }

    @Override
    protected Map<String,Resource> queryForData(String reportDate, String overflowLocations){
    	String url = String.format("Patient?_summary=true&death-date=%s&_has:Condition:patient:code=%s",
                reportDate,
                Config.getInstance().getTerminologyCovidCodes());
    	return this.search(url);
    }

	@Override
	public Map<String, Resource> getPatientConditions(Patient p) {
		// TODO Auto-generated method stub
		return null;
	}

}
