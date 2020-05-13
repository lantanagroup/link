package com.lantanagroup.nandina.query.fhir.r4.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;

import java.util.HashMap;
import java.util.Map;

import com.lantanagroup.nandina.query.AbstractQuery;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Resource;

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
		try {
			HospitalizedQuery hq = (HospitalizedQuery) this.getCachedQuery(config.getQueryHospitalized());
			EDOverflowQuery eq = (EDOverflowQuery) this.getCachedQuery(config.getQueryEDOverflow());
			Map<String,Resource> queryData = hq.getData(reportDate, overflowLocations);
			queryData.putAll(eq.getData(reportDate, overflowLocations));
			HashMap<String, Resource> finalPatientMap = deadPatients(queryData, reportDate);
			return finalPatientMap;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
			
		}
		
    	/*
    	String url = String.format("Patient?&death-date=%s&_has:Condition:patient:code=%s",
                reportDate,
                Config.getInstance().getTerminologyCovidCodes());
    	return this.search(url);
    	*/
    }
}
