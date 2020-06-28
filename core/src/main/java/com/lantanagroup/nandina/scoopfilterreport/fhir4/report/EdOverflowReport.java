package com.lantanagroup.nandina.scoopfilterreport.fhir4.report;

import java.util.List;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.CovidFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.EdOverflowEncounterFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.Filter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.scoop.EncounterScoop;

public class EdOverflowReport extends Report {

	public EdOverflowReport(EncounterScoop scoop, List<Filter> filters) {
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
