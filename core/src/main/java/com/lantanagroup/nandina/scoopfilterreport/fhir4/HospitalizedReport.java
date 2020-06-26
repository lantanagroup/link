package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.io.IOException;
import java.util.List;

public class HospitalizedReport extends Report {

	/*
	 *  Create this passing in an EncounterDateFilter for the date on which you are interested. 
	 */
	public HospitalizedReport(String fhirBaseUrl, Scoop scoop, List<Filter> filters) {
		super(fhirBaseUrl, scoop, addFilters(filters));
		// TODO Auto-generated constructor stub
	}
	
	
	protected static List<Filter> addFilters(List<Filter> filters){
		Filter covidFilter = new CovidFilter();
		filters.add(covidFilter);
		Filter hospitalizedFilter = new HospitalizedEncounterClassFilter();
		filters.add(hospitalizedFilter);
		return filters;
	}



}
