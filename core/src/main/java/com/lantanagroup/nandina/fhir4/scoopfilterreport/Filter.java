package com.lantanagroup.nandina.fhir4.scoopfilterreport;

import java.util.HashSet;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class Filter {
	

	protected static final Logger logger = LoggerFactory.getLogger(Filter.class);

	private Terminology terminology = new Terminology();
	
	public boolean isCovidPatient(PatientData pd) {
		boolean b = false;
		if (
				hasCovidCondition(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1124"))
				|| hasCovidCondition(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1203"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1142"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1144"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1152"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1153"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1154"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1157"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1158"))
				|| hasCovidLabTest(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1223"))
				|| hasCovidLabResult(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1143"))
				|| hasCovidLabResult(pd, terminology.getValueSetAsSetString("2.16.840.1.113762.1.4.1146.1155"))
				) {
			b = true;
		};
		return b;
	}
	

	private boolean hasCovidLabTest(PatientData pd, Set<String> codeSet) {
		boolean b = false;
		for (IBaseResource res : bundleToSet(pd.labResults)) {
			Observation c = (Observation)res;
			CodeableConcept cc = c.getCode();
			b = codeInSet(cc,codeSet);
			if (b) {
				logger.info("Patient has covid: " + pd.patient.getId());
				logger.info(" - " + cc.getCodingFirstRep().getCode());
				break;
			}
		}
		return b;
	}

	private boolean hasCovidLabResult(PatientData pd, Set<String> codeSet) {
		boolean b = false;
		for (IBaseResource res : bundleToSet(pd.labResults)) {
			Observation c = (Observation)res;
			CodeableConcept cc = c.getValueCodeableConcept();
			b = codeInSet(cc,codeSet);
			if (b) {
				logger.info("Patient has covid: " + pd.patient.getId());
				logger.info(" - " + cc.getCodingFirstRep().getCode());
				break;
			}
		}
		return b;
	}

	private boolean hasCovidCondition(PatientData pd, Set<String> codeSet) {
		boolean b = false;
		for (IBaseResource res : bundleToSet(pd.conditions)) {
			Condition c = (Condition)res;
			CodeableConcept cc = c.getCode();
			b = codeInSet(cc,codeSet);
			if (b) {
				logger.info("Patient has covid: " + pd.patient.getId());
				logger.info(" - " + cc.getCodingFirstRep().getCode());
				pd.primaryDx = cc;
				break;
			}
		}
		return b;
	}
	
	private boolean codeInSet(CodeableConcept cc, Set<String> covidCodesSet) {
		boolean b = false;
		for (Coding c : cc.getCoding()) {
			b = covidCodesSet.contains(c.getSystem() + "|" + c.getCode());
			if (b) break;
		}
		return b;
	}

	public Set<IBaseResource> bundleToSet(Bundle b){
		Set<IBaseResource> resSet = new HashSet<IBaseResource>();
		for (BundleEntryComponent entry : b.getEntry()) {
			resSet.add(entry.getResource());
		}
		return resSet;
	}

}
