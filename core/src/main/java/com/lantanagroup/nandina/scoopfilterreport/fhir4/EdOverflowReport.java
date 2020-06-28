package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.util.List;

public class EdOverflowReport extends Report {

	public EdOverflowReport(Scoop scoop, List<Filter> filters) {
		super(scoop, addFilters(filters));
		// TODO Auto-generated constructor stub
	}
	

	private static List<Filter> addFilters(List<Filter> filters){
		Filter covidFilter = new CovidFilter();
		filters.add(covidFilter);
		Filter edOverflowFilter = new EdOverflowEncounterFilter();
		filters.add(edOverflowFilter);
		return filters;
	}

}
