package com.lantanagroup.nandina.scoopfilterreport.fhir4.filter;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.PatientData;

public final class EdOverflowEncounterFilter extends HospitalizedEncounterFilter {
	
	/**
	 * Since Cerner does not support the Location resource, the best we can do now is the opposite of the HostpitalizedEncounterFilter. 
	 * This will almost certainly need revisiting once we see live Emory data. 
	 */
	@Override
	public boolean runFilter(PatientData pd) {
		return !super.runFilter(pd);
	}

}
