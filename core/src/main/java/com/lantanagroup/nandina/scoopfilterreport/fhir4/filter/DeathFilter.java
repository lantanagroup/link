package com.lantanagroup.nandina.scoopfilterreport.fhir4.filter;

import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.r4.model.Patient;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.PatientData;

public final class DeathFilter extends Filter {
	
	Date reportDate;
	
	public DeathFilter(Date reportDate) {
		super();
		this.reportDate = reportDate;
	}

	@Override
	public boolean runFilter(PatientData pd) {
		boolean dead = false;

		Patient p = (Patient) pd.getPatient();
		logger.debug("Checking if " + p.getId() + " died");
		if (p.hasDeceasedDateTimeType()) {
			Calendar deadDateCal = p.getDeceasedDateTimeType().toCalendar();
			Calendar reportDateCal = Calendar.getInstance();
			reportDateCal.setTime(reportDate);
			dead = sameDay(deadDateCal, reportDateCal);
		}
		return dead;
	}

	protected boolean sameDay(Calendar deadDate, Calendar reportDate) {
		boolean sameDay = false;
		if (deadDate.get(Calendar.YEAR) == reportDate.get(Calendar.YEAR)
				&& deadDate.get(Calendar.DAY_OF_YEAR) == reportDate.get(Calendar.DAY_OF_YEAR))
			sameDay = true;
		return sameDay;
	}

}
