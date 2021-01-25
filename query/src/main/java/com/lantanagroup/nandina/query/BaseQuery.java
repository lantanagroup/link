package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.lantanagroup.nandina.Helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.io.File;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lantanagroup.nandina.NandinaConfig;

import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

public abstract class BaseQuery implements IQueryCountExecutor {
	protected static final Logger logger = LoggerFactory.getLogger(BaseQuery.class);

	protected NandinaConfig properties;
	protected IGenericClient fhirClient;
	protected Map<String, String> criteria;
	protected Map<String, Object> contextData;

	public NandinaConfig getProperties() { return this.properties; }
	public void setProperties(NandinaConfig properties) { this.properties = properties; }

	public IGenericClient getFhirClient() { return this.fhirClient; }
	public void setFhirClient(IGenericClient fhirClient) { this.fhirClient = fhirClient; }

	public Map<String, String> getCriteria() { return this.criteria; }
	public void setCriteria(Map<String, String> criteria) { this.criteria = criteria; }

	public Map<String, Object> getContextData() { return this.contextData; }
	public void setContextData(Map<String, Object> contextData) { this.contextData = contextData; }

	public Object getContextData(String key) {
		return this.getContextData().get(key);
	}

	public void addContextData(String key, Object data) {
		this.getContextData().put(key, data);
	}

	// First string is a cache key, representing the criteria for the report,
	// including report date and locations
	// Second string represents the ID of the Resource
	// Resource represents the actual resource data for the ID

	protected Map<String, Resource> search(String query) {
		Bundle b = rawSearch(query);
		return bundleToMap(b);
	}

	protected Integer getCount(Map<String, Resource> resMap) {
		if (resMap == null) {
			return null;
		}
		return resMap.size();
	}

	public Bundle rawSearch(String query) {
		try {
			Bundle bundle = fhirClient.search().byUrl(query).returnBundle(Bundle.class).execute();

			logger.debug(this.getClass().getName() + " executing query: " + query);

			return bundle;
		} catch (AuthenticationException ae) {
			logger.error("Unable to perform rawSearch() with query " + query + " response body: " + ae.getResponseBody());
			ae.printStackTrace();
		} catch (Exception ex) {
			this.logger.error("Could not retrieve data for " + this.getClass().getName() + ": " + ex.getMessage(), ex);
		}
		return null;
	}

	private Map<String, Resource> bundleToMap(Bundle b) {
		if (b == null) {
			return null;
		}
		HashMap<String, Resource> resMap = new HashMap<String, Resource>();
		List<BundleEntryComponent> entryList = b.getEntry();
		for (BundleEntryComponent entry : entryList) {
			Resource res = entry.getResource();

			resMap.put(res.getIdElement().getIdPart(), res);
		}
		if (b.getLink(Bundle.LINK_NEXT) != null) {
			Bundle nextPage = fhirClient.loadPage().next(b).execute();
			resMap.putAll(bundleToMap(nextPage));
		}
		return resMap;
	}

	protected abstract Map<String, Resource> queryForData();

	public Map<String, Resource> getData() {
		String cacheKey = this.criteria.toString();

		if (this.cachedData != null && cachedData.containsKey(cacheKey)) {
			logger.debug(this.getClass().getName() + " returning cached data for date/overflow-locations: " + cacheKey);
			return cachedData.get(cacheKey);
		}
		Map<String, Resource> resMap = queryForData();
		if (resMap != null) {
			logger.debug(this.getClass().getName() + " getData() result count: " + resMap.size());
			cachedData.put(cacheKey, resMap);
		}
		return resMap;
	}

	protected Map<String, Resource> getPatientConditions(Patient p) {
		// TODO: Move verification-status codes to a value set and load thru config
		String condQuery = String.format(
				"Condition?verification-status=unconfirmed,provisional,differential,confirmed&code=%s&patient=Patient/%s",
				properties.getTerminologyCovidCodes(), p.getIdElement().getIdPart());
		Map<String, Resource> condMap = this.search(condQuery);
		return condMap;
	}

	protected Map<String, Resource> getPatientEncounters(Patient p) {
		// TODO: put encounter codes in config
		String encQuery = "Encounter?class=IMP,EMER,ACUTE,NONAC,OBSENC&subject=Patient/" + p.getIdElement().getIdPart();
		Map<String, Resource> encMap = this.search(encQuery);
		return encMap;
	}

	protected HashMap<String, Resource> filterPatientsByDate(String reportDate,
			Map<String, Patient> patientMap) throws ParseException {
		Set<String> keySet = patientMap.keySet();
		Date rDate = Helper.parseFhirDate(reportDate);
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patientId : keySet) {
			Map<String, Resource> encMap = this.getPatientEncounters((Patient) patientMap.get(patientId));
			Set<String> encKeySet = encMap.keySet();
			for (String encId : encKeySet) {
				Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encId).execute();
				Date start = encounter.getPeriod().getStart();
				if (start.before(rDate)) {
					logger.debug("Encounter start before reportDate");
					Date end = encounter.getPeriod().getEnd();
					if (end == null) {
						logger.debug("Encounter is ongoing");
						finalPatientMap.put(patientId, patientMap.get(patientId));
					} else if (end.after(rDate)) {
						logger.debug("Encounter end after reportDate");
						finalPatientMap.put(patientId, patientMap.get(patientId));
						break;
					} else {

						logger.debug("Encounter " + encounter.getId() + " ended after report date. Encounter end="
								+ Helper.getFhirDate(end));
					}
				} else {
					logger.debug("Encounter " + encounter.getId() + " started after report date. Encounter start="
							+ Helper.getFhirDate(start));
				}
			}
		}
		return finalPatientMap;
	}

	protected HashMap<String, Resource> filterPatientsByEncounterDate(String reportDate,
																	  Map<String, Resource> patientMap) throws ParseException {
		Set<String> keySet = patientMap.keySet();
		Date rDate = Helper.parseFhirDate(reportDate);
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patientId : keySet) {
			Map<String, Resource> encMap = this.getPatientEncounters((Patient) patientMap.get(patientId));
			Set<String> encKeySet = encMap.keySet();
			for (String encId : encKeySet) {
				Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encId).execute();
				Date start = encounter.getPeriod().getStart();
				if (start.before(rDate)) {
					logger.debug("Encounter start before reportDate");
					Date end = encounter.getPeriod().getEnd();
					if (end == null) {
						logger.debug("Encounter is ongoing");
						finalPatientMap.put(patientId, patientMap.get(patientId));
					} else if (end.after(rDate)) {
						logger.debug("Encounter end after reportDate");
						finalPatientMap.put(patientId, patientMap.get(patientId));
						break;
					} else {

						logger.debug("Encounter " + encounter.getId() + " ended after report date. Encounter end="
								+ Helper.getFhirDate(end));
					}
				} else {
					logger.debug("Encounter " + encounter.getId() + " started after report date. Encounter start="
							+ Helper.getFhirDate(start));
				}
			}
		}
		return finalPatientMap;
	}

	// TODO: Also search for patients with an intubation procedure during the
	// encounter
	protected HashMap<String, Resource> ventilatedPatients(Map<String, Resource> hqData) {
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		HashMap<String, Resource> ventilatorPatientMap = patientsOnVentilatorDevices(hqData);
		HashMap<String, Resource> intubationPatientMap = patientsWithIntubationProcedures(hqData);
		finalPatientMap.putAll(ventilatorPatientMap);
		finalPatientMap.putAll(intubationPatientMap);
		return finalPatientMap;
	}

	private HashMap<String, Resource> patientsOnVentilatorDevices(Map<String, Resource> hqData) {
		Set<String> patIds = hqData.keySet();
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patId : patIds) {
			String devQuery = String.format("Device?type=%s&patient=Patient/%s",
					properties.getTerminologyVentilatorCodes(), patId);
			Map<String, Resource> devMap = this.search(devQuery);
			if (devMap != null && devMap.size() > 0) {
				finalPatientMap.put(patId, hqData.get(patId));
			}

		}
		return finalPatientMap;
	}

	private HashMap<String, Resource> patientsWithIntubationProcedures(Map<String, Resource> hqData) {
		Set<String> patIds = hqData.keySet();
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patId : patIds) {
			String devQuery = String.format("Procedure?code=%s&patient=Patient/%s",
					properties.getTerminologyIntubationProcedureCodes(), patId);
			Map<String, Resource> devMap = this.search(devQuery);
			if (devMap != null && devMap.size() > 0) {
				finalPatientMap.put(patId, hqData.get(patId));
			}

		}
		return finalPatientMap;
	}

	protected HashMap<String, Resource> deadPatients(Map<String, Resource> queryData, String reportDateStr)
			throws ParseException {
		Set<String> patIds = queryData.keySet();
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		Calendar reportDate = Calendar.getInstance();
		reportDate.setTime(Helper.parseFhirDate(reportDateStr));
		for (String patId : patIds) {
			Patient p = (Patient) queryData.get(patId);
			logger.debug("Checking if " + patId + " died");
			if (p.hasDeceasedDateTimeType()) {
				Calendar deadDate = p.getDeceasedDateTimeType().toCalendar();
				if (DateUtils.isSameDay(deadDate.getTime(), reportDate.getTime())) {
					finalPatientMap.put(patId, p);
				}
			}
		}
		return finalPatientMap;
	}

	protected boolean sameDay(Calendar deadDate, Calendar reportDate) {
		boolean sameDay = false;
		if (deadDate.get(Calendar.YEAR) == reportDate.get(Calendar.YEAR)
				&& deadDate.get(Calendar.DAY_OF_YEAR) == reportDate.get(Calendar.DAY_OF_YEAR))
			sameDay = true;
		return sameDay;
	}
	

	protected Map<String, Map<String, Resource>> cachedData = new HashMap<String, Map<String, Resource>>();
	
	// The following was ported over from PillboxQuery without much fanfare. This entire class could use some refactoring around this stuff. 
	

	private Map<Patient, Encounter> patientEncounterMap = null;

	public Map<Patient, Encounter> getPatientEncounterMap() {
		return patientEncounterMap;
	}

	public Map<String,Patient> getPatientMap(Map<String,Encounter> encMap){
		Map<String,Patient> map = new HashMap<String,Patient>();
		patientEncounterMap = new HashMap<Patient,Encounter>();
		for (String key: encMap.keySet()) {
			Encounter enc = encMap.get(key);
			String subjectRef = enc.getSubject().getReference();
			if (subjectRef.startsWith("Patient/")) {
				Patient p = fhirClient.read().resource(Patient.class).withId(subjectRef).execute();
				map.put(p.getId(), p);
				patientEncounterMap.put(p, enc);
			} else {
				// It must be a Group, but Group can contain non-Patient resources, so deal with it later
				throw new RuntimeException("Encounter.subject of type Group not yet supported");
			}
		}
		return map;
	}


	
	public Bundle getEncounter(Identifier encId) {
		return rawSearch("Encounter?identifier=" + encId.getSystem() + "|" + encId.getValue()); 
	}

	public Map<String,Encounter> getEncounterMap(ListResource encList) {
		HashMap<String,Encounter> map = new HashMap<String,Encounter>();
		List<ListEntryComponent> entries = encList.getEntry();
		for (ListEntryComponent entry : entries) {
			
			List<Identifier> encIds = getIdentifiers(entry.getItem());
			for (Identifier encId : encIds) {
				Bundle search = getEncounter(encId);
				List<BundleEntryComponent> bEntries = search.getEntry();
				for (BundleEntryComponent bEntry : bEntries) {
					Encounter retrievedEnc = (Encounter)bEntry.getResource();
					map.put(retrievedEnc.getId(), retrievedEnc);
				}
			}
		}
		return map;
	}

	private List<Identifier> getIdentifiers(Reference item) {
		List<Identifier> identifiers;
		if(item.hasReference()) {
			BaseResource res = (BaseResource) item.getResource();
			String type = item.getType();
			switch(type) {
				case "Encounter":
					identifiers = ((Encounter)res).getIdentifier();
				case "Patient":
					identifiers = ((Patient)res).getIdentifier();
				default:
					throw new RuntimeException("Update code to get identifiers for " + type);
			}
		} else {
			identifiers = new ArrayList<Identifier>();
			identifiers.add(item.getIdentifier());
		}
		return identifiers;
	}
	

    
    private Set<String> valueSetToSet(ValueSet vs){
    	Set<String> set = new HashSet<String>();
    	if (vs.getExpansion().hasContains()) {
        	for (ValueSetExpansionContainsComponent contains: vs.getExpansion().getContains()) {
        		Set<String> c = extractContainsSet(contains);
        		set.addAll(c);
        	}
    	}
    	return set;
    }
    

    
    private Set<String> extractContainsSet(ValueSetExpansionContainsComponent contains) {
    	Set<String> set = new HashSet<String>();
    	if (contains.hasSystem() && contains.hasCode()) {
        	set.add(contains.getSystem() + "|" + contains.getCode());
    	}
    	if (contains.hasContains()) {
        	for (ValueSetExpansionContainsComponent childContains: contains.getContains()) {
        		Set<String> c = extractContainsSet(childContains);
        		set.addAll(c);
        	}
    	}
		return set;
	}
    

    private Map<String,Set<String>> codeSets = new HashMap<String,Set<String>>();
    
    public Set<String> getValueSetAsSetString(String oid){
    	Set<String> codeSet = null;
    	if (codeSets.containsKey(oid)) {
    		codeSet = codeSets.get(oid); 
    	} else {
    		ValueSet vs = getValueSet(oid);
    		codeSet = valueSetToSet(vs);
    		codeSets.put(oid, codeSet);
    	}
    	return codeSet;
    }
    
    private ValueSet getValueSet(String oid) {
    	String uri = "classpath:" + oid + ".xml";
    	ValueSet vs = loadValueSet(uri);
    	return vs;
    }
    

	private FhirContext ctx = FhirContext.forR4();
	private IParser xmlParser = ctx.newXmlParser();

	private ValueSet loadValueSet(String valueSetUri) {
    	ValueSet vs = null;
        logger.info("Loading ValueSet " + valueSetUri);
        try {
            if (valueSetUri.startsWith("https://") || valueSetUri.startsWith("http://")) {
                // TODO: pull the value set from the web 
            	// or look up cannonical http/https value set URIs from a FHIR server. 
            	// Just because ValueSet.uri starts with http does not mean it is actually accessible at that URL
                throw new Exception("Nandina does not yet support http/https value sets.");
            } else {
                File valueSetFile = ResourceUtils.getFile(valueSetUri);
                String vsString = new String(Files.readAllBytes(valueSetFile.toPath()));
                vs = (ValueSet) xmlParser.parseResource(vsString);
            }
        } catch (Exception ex) {
            logger.error("Could not load " + valueSetUri, ex);
        }

        logger.info(String.format("Loaded ValueSet %s", valueSetUri));
        return vs;
    }

	
}
