package com.lantanagroup.nandina.query.fhir.r4.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import com.lantanagroup.nandina.IConfig;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;

public class HospitalOnsetQuery extends AbstractQuery implements IQueryCountExecutor {
	
    public HospitalOnsetQuery(JsonProperties jsonProperties, IGenericClient fhirClient, HashMap<String, String> criteria) {
		super(jsonProperties, fhirClient, criteria);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Integer execute() {
    	if (!this.criteria.containsKey("reportDate") && !this.criteria.containsKey("overflowLocations")) {
    		return null;
		}

		Map<String,Resource> resMap = this.getData();
	    return this.getCount(resMap);
	}
	
	@Override
	protected Map<String,Resource> queryForData() {
		try {
			String reportDate = this.criteria.get("reportDate");
			String overflowLocations = this.criteria.get("overflowLocations");

			String hClass = jsonProperties.getQuery().get(JsonProperties.HOSPITALIZED);
			HospitalizedQuery hq = (HospitalizedQuery) this.getCachedQuery(hClass);
			Map<String,Resource> hqData = hq.getData();
			HashMap<String, Resource> finalPatientMap = getHospitalOnsetPatients(reportDate, overflowLocations, hqData);
			return finalPatientMap;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new RuntimeException(e);
		}
	}

	private HashMap<String, Resource> getHospitalOnsetPatients(String reportDate, String overflowLocations, Map<String,Resource> hqData) {
		Set<String> patIds = hqData.keySet();
		HashMap<String, Resource> finalPatientMap = new HashMap<String, Resource>();
		for (String patId : patIds) {
			Patient p = (Patient) hqData.get(patId);
			if (isHospitalOnset(p)) {
				finalPatientMap.put(patId, p);
			}
			
		}
		return finalPatientMap;
	}
	
	private boolean isHospitalOnset(Patient p) {
		boolean hospitalOnset = false;
		Map<String,Resource> condMap = getPatientConditions(p);
		Map<String,Resource> encMap = getPatientEncounters(p);
		if (condMap != null && condMap.size() > 0) {
			for (String condId : condMap.keySet()) {
				Condition cond = (Condition) condMap.get(condId);
				for (String encId : encMap.keySet()) {
					Encounter enc = (Encounter) encMap.get(encId);
					hospitalOnset = onsetDuringEncounter(cond,enc);
				}
			}
		}
		return hospitalOnset;
	}

	private boolean onsetDuringEncounter(Condition cond, Encounter enc) {
		boolean hospitalOnset = false;
		Period period = enc.getPeriod();
		if (period != null) {

			Date encStart = enc.getPeriod().getStart();
			Date encEnd = enc.getPeriod().getEnd();
			
			if (cond.hasOnsetDateTimeType()) {
				Calendar onsetDate = cond.getOnsetDateTimeType().toCalendar();
				hospitalOnset = onsetDuringEncounter(onsetDate, encStart, encEnd);
			} else if (cond.hasOnsetPeriod()) {
				Calendar onsetPeriodStart = cond.getOnsetPeriod().getStartElement().toCalendar();
				hospitalOnset = onsetDuringEncounter(onsetPeriodStart, encStart, encEnd);
				if (hospitalOnset == false) {
					Calendar onsetPeriodEnd = cond.getOnsetPeriod().getEndElement().toCalendar();
					hospitalOnset = onsetDuringEncounter(onsetPeriodEnd, encStart, encEnd);
				}
			}
		}
		// TODO: at some point maybe try to deal with Condition.onsetRange if the UCUM unit is something time related, but would needs to see some real world sample data that shows this is necessary.
		return hospitalOnset;
	}

	private boolean onsetDuringEncounter(Calendar onsetDate, Date encStartDate, Date encEndDate) {
		boolean hospitalOnset = false;
		if (encStartDate != null) {
			Calendar encStartPlus14 = Calendar.getInstance();
			encStartPlus14.setTime(encStartDate);
			encStartPlus14.roll(Calendar.DAY_OF_YEAR, 14);
			if (onsetDate.after(encStartPlus14)) {
				hospitalOnset = true;
				if (encEndDate != null) {
					Calendar encEnd = Calendar.getInstance();
					encEnd.setTime(encEndDate);
					if (onsetDate.after(encEnd)) {
						// onset after discharge
						hospitalOnset = false;
					}
					
				}
			}
		}
		return hospitalOnset;
	}
}
