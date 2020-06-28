package com.lantanagroup.nandina.scoopfilterreport.fhir4.report;

import java.util.List;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.Filter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.filter.VentilatedFilter;
import com.lantanagroup.nandina.scoopfilterreport.fhir4.scoop.EncounterScoop;

public class HospitalizedAndVentilatedReport extends HospitalizedReport {

	public HospitalizedAndVentilatedReport(EncounterScoop scoop,List<Filter> filters) {
		super(scoop, addFilters(filters));
		// TODO Auto-generated constructor stub
	}
	
	
	private static List<Filter> addFilters(List<Filter> filters){
		Filter ventilatedFilter = new VentilatedFilter();
		filters.add(ventilatedFilter);
		return filters;
	}

}
