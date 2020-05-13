package com.lantanagroup.nandina.query.fhir.r4.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;

import java.util.HashMap;
import java.util.Map;

import com.lantanagroup.nandina.query.AbstractQuery;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Resource;

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
