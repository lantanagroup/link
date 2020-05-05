package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalOnsetQuery extends AbstractQuery implements IQueryCountExecutor {
	
    public HospitalOnsetQuery(IConfig config, IGenericClient fhirClient) {
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
			HashMap<String, Resource> finalPatientMap = getHospitalOnsetPatients(reportDate, overflowLocations, hqData);
			return finalPatientMap;
			// Old query below
			/*
			String url = String.format("Patient?_summary=true&_has:Condition:patient:code=%s&_has:Encounter:patient:class=IMP&_has:Encounter:patient:status=in-progress&_has:Encounter:patient:date=le%s",
	                Config.getInstance().getTerminologyCovidCodes(),
	                encounterDateStart);
			return this.search(url);
			*/
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
		Calendar encStart = enc.getPeriod().getStartElement().toCalendar();
		Calendar encEnd = enc.getPeriod().getEndElement().toCalendar();
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
		// TODO: at some point maybe try to deal with Condition.onsetRange if the UCUM unit is something time related, but would needs to see some real world sample data that shows this is necessary.
		return hospitalOnset;
	}

	private boolean onsetDuringEncounter(Calendar onsetDate, Calendar encStart, Calendar encEnd) {
		boolean hospitalOnset = false;
		Calendar encStartPlus14 = Calendar.getInstance();
		encStartPlus14.setTime(encStart.getTime());
		encStartPlus14.roll(Calendar.DAY_OF_YEAR, 14);
		if (onsetDate.after(encStartPlus14) && onsetDate.before(encEnd)) {
			hospitalOnset = true;
		}
		return hospitalOnset;
	}

    
}
