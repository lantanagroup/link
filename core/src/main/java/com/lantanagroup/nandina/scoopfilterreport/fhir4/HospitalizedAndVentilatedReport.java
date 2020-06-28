package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.util.List;

public class HospitalizedAndVentilatedReport extends HospitalizedReport {

	public HospitalizedAndVentilatedReport(String fhirBaseUrl, List<PatientData> initialPatientData,
			List<Filter> filters) {
		super(fhirBaseUrl, initialPatientData, addFilters(filters));
		// TODO Auto-generated constructor stub
	}
	

	
	
	private static List<Filter> addFilters(List<Filter> filters){
		Filter ventilatedFilter = new VentilatedFilter();
		filters.add(ventilatedFilter);
		return filters;
	}

}
