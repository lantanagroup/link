package com.lantanagroup.nandina.scoopfilterreport.fhir4.report;

import java.util.List;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.CovidFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.Filter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.HospitalizedEncounterFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.scoop.EncounterScoop;

public class HospitalizedReport extends Report {

	/*
	 *  Create this passing in an EncounterDateFilter for the date on which you are interested. 
	 */
	public HospitalizedReport(EncounterScoop scoop, List<Filter> filters) {
		super(scoop, addFilters(filters));
	}
	
	
	private static List<Filter> addFilters(List<Filter> filters){
		Filter covidFilter = new CovidFilter();
		filters.add(covidFilter);
		Filter hospitalizedFilter = new HospitalizedEncounterFilter();
		filters.add(hospitalizedFilter);
		return filters;
	}



}
