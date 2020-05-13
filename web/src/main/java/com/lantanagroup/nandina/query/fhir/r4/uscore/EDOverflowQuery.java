package com.lantanagroup.nandina.query.fhir.r4.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.IConfig;

import java.util.HashMap;
import java.util.Map;

import com.lantanagroup.nandina.query.AbstractQuery;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Resource;

public class EDOverflowQuery extends AbstractQuery implements IQueryCountExecutor {

    public EDOverflowQuery(IConfig config, IGenericClient fhirClient) {
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
		try {
	    	String url = String.format("Patient?_has:Condition:patient:code=%s&_has:Encounter:patient:location=%s:date=ge%s,le%s",
	                Config.getInstance().getTerminologyCovidCodes(),
	                overflowLocations,reportDate,reportDate);
			Map<String, Resource> patientMap = this.search(url);
			// Encounter.date search parameter not working with current release of HAPI, so
			// weeding out encounters outside the reportDate manually
			HashMap<String, Resource> finalPatientMap = filterPatientsByEncounterDate(reportDate, patientMap);
			return finalPatientMap;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
    	/*
    	String url = String.format("Patient?_summary=true&_has:Condition:patient:code=%s&_has:Encounter:patient:location=%s:date=ge%s,le%s",
                Config.getInstance().getTerminologyCovidCodes(),
                overflowLocations,reportDate,reportDate);
    	return this.search(url);
    	*/
    }

}
