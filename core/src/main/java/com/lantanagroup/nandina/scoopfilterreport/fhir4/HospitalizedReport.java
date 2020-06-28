package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.util.List;

public class HospitalizedReport extends Report {

	/*
	 *  Create this passing in an EncounterDateFilter for the date on which you are interested. 
	 */
	public HospitalizedReport(String fhirBaseUrl, List<PatientData> initialPatientData, List<Filter> filters) {
		super(fhirBaseUrl, initialPatientData, addFilters(filters));
	}
	
	
	private static List<Filter> addFilters(List<Filter> filters){
		Filter covidFilter = new CovidFilter();
		filters.add(covidFilter);
		Filter hospitalizedFilter = new HospitalizedEncounterClassFilter();
		filters.add(hospitalizedFilter);
		return filters;
	}



}
