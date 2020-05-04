package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDOverflowAndVentilatedQuery extends AbstractQuery implements IQueryCountExecutor {
	
    public EDOverflowAndVentilatedQuery(IConfig config, IGenericClient fhirClient) {
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
        String url = String.format("Patient?_summary=true&_has:Condition:patient:code=%s&_has:Encounter:patient:location=%s&_has:Device:patient:type=%s",
                Config.getInstance().getTerminologyCovidCodes(),
                overflowLocations,
                Config.getInstance().getTerminologyDeviceTypeCodes());
    	return this.search(url);
    }
    
}
