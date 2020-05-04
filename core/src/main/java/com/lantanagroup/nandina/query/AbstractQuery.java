package com.lantanagroup.nandina.query;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractQuery implements IQueryCountExecutor{

    private static final String NO_DEVICE_CODES_ERROR = "Device-type codes have not been specified in configuration.";
	private static final String NO_COVID_CODES_ERROR = "Covid codes have not been specified in configuration.";
	protected static final Logger logger = LoggerFactory.getLogger(AbstractQuery.class);
	protected static HashMap<String,AbstractQuery> cachedQueries = new HashMap<String,AbstractQuery>();
	
	protected IConfig config;
	protected IGenericClient fhirClient;
	protected Map<String,Resource> cachedData = null;
	
	public AbstractQuery(IConfig config, IGenericClient fhirClient) {
		logger.info("Instantiating class: " + this.getClass());
		this.config = config;
		this.fhirClient = fhirClient;
        if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
            this.logger.error(NO_COVID_CODES_ERROR);
            throw new RuntimeException(NO_COVID_CODES_ERROR);
        }
        if (Helper.isNullOrEmpty(config.getTerminologyDeviceTypeCodes())) {
            this.logger.error(NO_DEVICE_CODES_ERROR);
            throw new RuntimeException(NO_DEVICE_CODES_ERROR);
        }
        cachedQueries.put(this.getClass().getName(), this);
	}
	
	protected Map<String,Resource> search(String query){
		Bundle b = rawSearch(query);
		return bundleToMap(b);
	}
	
	protected Integer getCount(Map<String,Resource> resMap) {
		if (resMap == null) {
			return null;
		}
		return resMap.size();
	}
	
	private Bundle rawSearch(String query) {
        try {
            String url = String.format(query);
            Bundle deathsBundle = fhirClient.search()
                    .byUrl(url)
                    .returnBundle(Bundle.class)
                    .execute();

        	logger.info(this.getClass().getName() + " executing query: " + url);
            return deathsBundle;
        } catch (Exception ex) {
            this.logger.error("Could not retrieve data for "  + this.getClass().getName() +  ": " + ex.getMessage(), ex);
        }
        return null;
	}
	
	private Map<String,Resource> bundleToMap(Bundle b){
		if (b == null) {
			return null;
		}
		HashMap<String,Resource> resMap = new HashMap<String,Resource>();
		List<BundleEntryComponent> entryList = b.getEntry();
		for (BundleEntryComponent entry: entryList) {
			Resource res = entry.getResource();
			
			resMap.put(res.getIdElement().getIdPart(), res);
		}
		if (b.getLink(Bundle.LINK_NEXT) != null) {
			  Bundle nextPage = fhirClient.loadPage().next(b).execute();
			  resMap.putAll(bundleToMap(nextPage));
		}
		return resMap;
	}
	
	protected abstract Map<String,Resource> queryForData(String reportDate, String overflowLocations);
    
    protected Map<String,Resource> getData(String reportDate, String overflowLocations) {
    	if (this.cachedData != null) {
    		return cachedData;
    	} 
    	Map<String,Resource> resMap = queryForData(reportDate, overflowLocations);
    	if(resMap != null) {
        	logger.info(this.getClass().getName() + " getData() result count: " + resMap.size());
    	}
    	return resMap;
    }
    
    protected AbstractQuery getCachedQuery(String queryClass) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	if (cachedQueries.containsKey(queryClass)) {
    		return cachedQueries.get(queryClass);
    	} else {
    		return QueryFactory.newInstance(queryClass, config, fhirClient);
    	}
    }
	

}
