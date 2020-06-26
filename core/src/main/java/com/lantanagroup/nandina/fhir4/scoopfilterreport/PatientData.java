package com.lantanagroup.nandina.fhir4.scoopfilterreport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lantanagroup.nandina.query.fhir.r4.AbstractQuery;
import ca.uhn.fhir.context.FhirContext;

public class PatientData {

	protected static FhirContext ctx = FhirContext.forR4();
	protected static final Logger logger = LoggerFactory.getLogger(PatientData.class);
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	Date dateCollected;
	Patient patient;
	Encounter encounter;
	Bundle encounters;
	Bundle conditions;
	Bundle meds;
	Bundle labResults;
	Bundle allergies;
	CodeableConcept primaryDx = null;
	
	public PatientData(Scoop scoop, Patient pat) {
		patient = pat;
		dateCollected = new Date();
		Map<Patient,Encounter> patEncMap = scoop.getPatientEncounterMap();
		encounter = patEncMap.get(patient);
		encounters = scoop.rawSearch("Encounter?subject=" + pat.getId()); 
		conditions = scoop.rawSearch("Condition?subject=" + pat.getId()); 
		meds = scoop.rawSearch("MedicationRequest?subject=" + pat.getId()); 
		labResults = scoop.rawSearch("Observation?subject=" + pat.getId() + "&category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory"); 
		allergies = scoop.rawSearch("AllergyIntolerance?patient=" + pat.getId()); 
	}

	
	public Bundle getBundle() {
		Bundle b = new Bundle();
		b.setType(BundleType.COLLECTION);
		b.addEntry().setResource(patient);
		b.addEntry().setResource(encounters);
		b.addEntry().setResource(conditions);
		b.addEntry().setResource(meds);
		b.addEntry().setResource(labResults);
		b.addEntry().setResource(allergies);
		return b;
	}
	
}