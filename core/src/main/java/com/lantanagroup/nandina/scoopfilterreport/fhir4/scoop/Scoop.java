package com.lantanagroup.nandina.scoopfilterreport.fhir4.scoop;

import java.util.Date;
import java.util.List;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.PatientData;

public abstract class Scoop {
	
	protected List<PatientData> patientData;
	protected Date reportDate = null;

	public List<PatientData> getPatientData() {
		return patientData;
	}

	public Date getReportDate() {
		return reportDate;
	}

}
