package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
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
			String hClass = config.getQueryHospitalized();
			HospitalizedQuery hq = (HospitalizedQuery) this.getCachedQuery(hClass);
			Map<String,Resource> hqData = hq.getData(reportDate, overflowLocations);
			Set<String> patIds = hqData.keySet();
			HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
			for (String patId : patIds) {
				String devQuery = String.format("Device?type=%s&patient=Patient/%s", Config.getInstance().getTerminologyCovidCodes(), patId);
				Map<String, Resource> devMap = this.search(devQuery);
				if (devMap != null && devMap.size() > 0) {
					finalPatientMap.put(patId, hqData.get(patId));
				}
				
			}
			return finalPatientMap;
			// Old query below
			/*
		    String url = String.format("Patient?_summary=true&_active=true&_has:Condition:patient:code=%s&_has:Device:patient:type=%s",
		      Config.getInstance().getTerminologyCovidCodes(),
		      Config.getInstance().getTerminologyDeviceTypeCodes());
			return this.search(url);
			*/
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	}

}
