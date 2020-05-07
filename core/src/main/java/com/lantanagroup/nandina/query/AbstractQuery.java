package com.lantanagroup.nandina.query;

import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractQuery implements IQueryCountExecutor{

    private static final String NO_DEVICE_CODES_ERROR = "Device-type codes have not been specified in configuration.";
	private static final String NO_COVID_CODES_ERROR = "Covid codes have not been specified in configuration.";
	protected static final Logger logger = LoggerFactory.getLogger(AbstractQuery.class);
	protected static HashMap<String,AbstractQuery> cachedQueries = new HashMap<String,AbstractQuery>();
	
	public final Calendar dateCreated = Calendar.getInstance();
	protected IConfig config;
	protected IGenericClient fhirClient;
	// TODO: Change to allow caching by date
	protected Map<String, Map<String,Resource>> cachedData = new HashMap<String, Map<String,Resource>>();
	
	public AbstractQuery(IConfig config, IGenericClient fhirClient) {
		logger.info("Instantiating class: " + this.getClass());
		this.config = config;
		this.fhirClient = fhirClient;
        if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
            this.logger.error(NO_COVID_CODES_ERROR);
            throw new RuntimeException(NO_COVID_CODES_ERROR);
        }
        if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
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
    	String cacheKey = reportDate + "/" + overflowLocations;
    	if (this.cachedData != null && cachedData.containsKey(cacheKey)) {
        	logger.info(this.getClass().getName() + " returning cached data for date/overflow-locations: " + cacheKey);
    		return cachedData.get(cacheKey);
    	} 
    	Map<String,Resource> resMap = queryForData(reportDate, overflowLocations);
    	if(resMap != null) {
        	logger.info(this.getClass().getName() + " getData() result count: " + resMap.size());
        	cachedData.put(cacheKey,resMap);
        //	cachedData = resMap;
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

	protected Map<String, Resource> getPatientConditions(Patient p) {
		String condQuery = String.format("Condition?code=%s&patient=Patient/%s", config.getTerminologyCovidCodes(), p.getIdElement().getIdPart());
		Map<String, Resource> condMap = this.search(condQuery);
		return condMap;
	}
	
	protected Map<String,Resource> getPatientEncounters(Patient p){
		// TODO: put encounter codes in config
		String encQuery = "Encounter?class=IMP,EMER,ACUTE,NONAC,OBSENC&subject=Patient/" + p.getIdElement().getIdPart();
		Map<String, Resource> encMap = this.search(encQuery);
		return encMap;
	}
	

	protected HashMap<String, Resource> filterPatientsByEncounterDate(String reportDate, Map<String, Resource> patientMap)
			throws ParseException {
		Set<String> keySet = patientMap.keySet();
		Date rDate = Helper.parseFhirDate(reportDate);
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patientId : keySet) {
			Map<String, Resource> encMap = this.getPatientEncounters((Patient)patientMap.get(patientId));
			Set<String> encKeySet = encMap.keySet();
			for (String encId : encKeySet) {
				Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encId).execute();
				Date start = encounter.getPeriod().getStart();
				if (start.before(rDate)) {
					logger.info("Encounter start before reportDate");
					Date end = encounter.getPeriod().getEnd();
					if (end == null) {
						logger.info("Encounter is ongoing");
						finalPatientMap.put(patientId, patientMap.get(patientId));
					} else if (end.after(rDate)) {
						logger.info("Encounter end after reportDate");
						finalPatientMap.put(patientId, patientMap.get(patientId));
						break;
					} else {

						logger.info("Encounter " + encounter.getId() + " ended after report date. Encounter end=" + Helper.getFhirDate(end));
					}
				} else {
					logger.info("Encounter " + encounter.getId() + " started after report date. Encounter start=" + Helper.getFhirDate(start));
				}
			}
		}
		return finalPatientMap;
	}

	// TODO: Also search for patients with an intubation procedure during the encounter
	protected HashMap<String, Resource> ventilatedPatients(Map<String, Resource> hqData) {
		Set<String> patIds = hqData.keySet();
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patId : patIds) {
			String devQuery = String.format("Device?type=%s&patient=Patient/%s", config.getTerminologyVentilatorCodes(), patId);
			Map<String, Resource> devMap = this.search(devQuery);
			if (devMap != null && devMap.size() > 0) {
				finalPatientMap.put(patId, hqData.get(patId));
			}
			
		}
		return finalPatientMap;
	}

	protected HashMap<String, Resource> deadPatients(Map<String, Resource> queryData, String reportDateStr) throws ParseException {
		Set<String> patIds = queryData.keySet();
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		Calendar reportDate = Calendar.getInstance();
		reportDate.setTime(Helper.parseFhirDate(reportDateStr));
		for (String patId : patIds) {
			Patient p = (Patient)queryData.get(patId);
			logger.info("Checking if " + patId + " died");
			if (p.hasDeceasedDateTimeType()) {
				Calendar deadDate = p.getDeceasedDateTimeType().toCalendar();
				boolean sameDay = sameDay(deadDate, reportDate);
				if (sameDay) {
					finalPatientMap.put(patId, p);
				}
			}
		}
		return finalPatientMap;
	}

	protected boolean sameDay(Calendar deadDate, Calendar reportDate) {
		boolean sameDay = false;
		if (
				deadDate.get(Calendar.YEAR) == reportDate.get(Calendar.YEAR)
				&& 
				deadDate.get(Calendar.DAY_OF_YEAR) == reportDate.get(Calendar.DAY_OF_YEAR)
			) sameDay = true;
		return sameDay;
	}

}
