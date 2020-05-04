package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.Map;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalOnsetQuery extends AbstractQuery implements IQueryCountExecutor {
	
    public HospitalOnsetQuery(IConfig config, IGenericClient fhirClient) {
		super(config, fhirClient);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Integer execute(String reportDate, String overflowLocations) {
		Map<String,Resource> resMap = this.getData(reportDate, overflowLocations);
	    return this.getCount(resMap);
	}
	
	@Override
	protected Map<String,Resource> queryForData(String reportDate, String overflowLocations){
        // TODO: This query needs work
		/*
		String url = String.format("Patient?_summary=true&_has:Condition:patient:code=%s&_has:Encounter:patient:class=IMP&_has:Encounter:patient:status=in-progress&_has:Encounter:patient:date=le%s",
                Config.getInstance().getTerminologyCovidCodes(),
                encounterDateStart);
		return this.search(url);
		*/
		return null;
	}
    
}
