package com.lantanagroup.nandina.scoopfilterreport.fhir4.report;

import java.util.List;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.Filter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.VentilatedFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.scoop.EncounterScoop;

public class EdverflowAndVentilatedReport extends EdOverflowReport {

	public EdverflowAndVentilatedReport(EncounterScoop scoop, List<Filter> filters) {
		super(scoop, addFilters(filters));
	}
	
	private static List<Filter> addFilters(List<Filter> filters){
		Filter ventilatedFilter = new VentilatedFilter();
		filters.add(ventilatedFilter);
		return filters;
	}

}
