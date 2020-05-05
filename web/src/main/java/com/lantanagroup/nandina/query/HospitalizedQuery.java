package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalizedQuery extends AbstractQuery implements IQueryCountExecutor {
	public HospitalizedQuery(IConfig config, IGenericClient fhirClient) {
		super(config, fhirClient);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Integer execute(String reportDate, String overflowLocations) {
		Map<String, Resource> resMap = this.getData(reportDate, overflowLocations);
		return this.getCount(resMap);
	}

	@Override
	protected Map<String, Resource> queryForData(String reportDate, String overflowLocations) {
		try {
			String url = String.format(
					"Patient?_summary=true&_active=true&_has:Condition:patient:code=%s&_has:Encounter:patient:class=IMP,EMER,ACUTE,NONAC,OBSENC",
					config.getTerminologyCovidCodes());
			Map<String, Resource> patientMap = this.search(url);
			// Encounter.date search parameter not working with current release of HAPI, so
			// weeding out encounters outside the reportDate manually
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
						if (end.after(rDate)) {
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

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Resource> getPatientConditions(Patient p) {
		// TODO Auto-generated method stub
		return null;
	}

}
