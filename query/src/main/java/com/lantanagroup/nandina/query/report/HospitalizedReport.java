package com.lantanagroup.nandina.query.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.filter.CovidFilter;
import com.lantanagroup.nandina.query.filter.Filter;
import com.lantanagroup.nandina.query.filter.HospitalizedEncounterFilter;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;

import java.util.ArrayList;
import java.util.List;

public class HospitalizedReport extends Report {
	
	public HospitalizedReport(EncounterScoop scoop, FhirContext ctx) {
		super(scoop, addFilters(new ArrayList<Filter>()), ctx);
	}

	public HospitalizedReport(EncounterScoop scoop, List<Filter> filters, FhirContext ctx) {
		super(scoop, addFilters(filters), ctx);
	}

	private static List<Filter> addFilters(List<Filter> filters) {
		Filter covidFilter = new CovidFilter();
		filters.add(covidFilter);
		Filter hospitalizedFilter = new HospitalizedEncounterFilter();
		filters.add(hospitalizedFilter);
		return filters;
	}

}
