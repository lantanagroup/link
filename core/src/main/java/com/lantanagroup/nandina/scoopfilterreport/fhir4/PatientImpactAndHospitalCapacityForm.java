package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.sql.Date;
import java.util.Calendar;

public class PatientImpactAndHospitalCapacityForm {
	

	private HospitalizedReport hospitalized;
	private HospitalizedAndVentilatedReport hospitalizedAndVentilated;
	private HospitalizedReport previousDayHospitalized;
	private HospitalizedAndVentilatedReport previousDayHospitalizedAndVentilated;
	
	public PatientImpactAndHospitalCapacityForm(String fhirBaseUrl, Date reportDate) {
		EncounterDateFilter encounterDateFilter = new EncounterDateFilter(reportDate);
		Calendar cal = Calendar.getInstance();
		cal.setTime(reportDate);
		cal.roll(Calendar.DATE, -1);
		EncounterDateFilter previousDayFilter = new EncounterDateFilter(cal.getTime());
		
		
	}
	
	public int getHospitalized() {
		return hospitalized.getReportCount();
	}

	public HospitalizedAndVentilatedReport getHospitalizedAndVentilated() {
		return hospitalizedAndVentilated;
	}

}
