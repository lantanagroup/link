package com.lantanagroup.nandina.scoopfilterreport.fhir4;

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

public abstract class Filter {
	

	protected static final Logger logger = LoggerFactory.getLogger(Filter.class);
	protected Terminology terminology = new Terminology();
	public abstract boolean runFilter(PatientData pd);
	
	protected boolean codeInSet(CodeableConcept cc, Set<String> covidCodesSet) {
		boolean b = false;
		for (Coding c : cc.getCoding()) {
			b = covidCodesSet.contains(c.getSystem() + "|" + c.getCode());
			if (b) break;
		}
		return b;
	}

	protected Set<IBaseResource> bundleToSet(Bundle b){
		Set<IBaseResource> resSet = new HashSet<IBaseResource>();
		for (BundleEntryComponent entry : b.getEntry()) {
			resSet.add(entry.getResource());
		}
		return resSet;
	}

}
