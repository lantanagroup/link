package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Report {
	
	protected static final Logger logger = LoggerFactory.getLogger(Report.class);
	protected List<PatientData> patientData;
	
	public Report (String fhirBaseUrl, Scoop scoop, List<Filter> filters) {
		patientData = new ArrayList<PatientData>();
		for (String key : scoop.getPatientMap().keySet()) {
			PatientData pd;
			try {
				Patient p = scoop.getPatientMap().get(key);
				pd = new PatientData(scoop, p);
				for (Filter filter: filters) {
					if (filter.runFilter(pd)) {
						patientData.add(pd);
					}
				}
			} catch (Exception e) {
				logger.info("Error loading data for " + key, e);
			} 
		}
	}
	
	public abstract byte[] getReportData() throws IOException;

}
