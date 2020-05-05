package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalizedAndVentilatedQuery extends AbstractQuery implements IQueryCountExecutor {

  public HospitalizedAndVentilatedQuery(IConfig config, IGenericClient fhirClient) {
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
			HospitalizedQuery hq = (HospitalizedQuery) this.getCachedQuery(config.getQueryHospitalized());
			Map<String,Resource> hqData = hq.getData(reportDate, overflowLocations);
			HashMap<String, Resource> finalPatientMap = ventilatedPatients(hqData);
			return finalPatientMap;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	}




}
