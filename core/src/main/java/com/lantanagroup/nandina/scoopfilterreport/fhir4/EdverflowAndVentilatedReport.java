package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.util.List;

public class EdverflowAndVentilatedReport extends EdOverflowReport {

	public EdverflowAndVentilatedReport(Scoop scoop, List<Filter> filters) {
		super(scoop, addFilters(filters));
	}
	
	private static List<Filter> addFilters(List<Filter> filters){
		Filter ventilatedFilter = new VentilatedFilter();
		filters.add(ventilatedFilter);
		return filters;
	}

}
