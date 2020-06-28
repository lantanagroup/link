package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Scoop {

	protected static final Logger logger = LoggerFactory.getLogger(Scoop.class);
	protected IParser xmlParser;
	protected IGenericClient targetFhirServer;
	protected IGenericClient nandinaFhirServer;
	protected HashMap<String,Encounter> encounterMap = new HashMap<String,Encounter>(); 
	protected Map<String,Patient> patientMap = new HashMap<String,Patient>(); 
	protected Map<Patient,Encounter> patientEncounterMap = new HashMap<Patient,Encounter>();
	protected List<PatientData> patientData;
	protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");	
	protected IValidationSupport validationSupport;
	protected FHIRPathEngine fpe;
	


	public Scoop (IGenericClient targetFhirServer, IGenericClient nandinaFhirServer, ListResource encList) {
		this.targetFhirServer = targetFhirServer;
		this.nandinaFhirServer = nandinaFhirServer;
		init(encList);
	}
	
	public Scoop (IGenericClient targetFhirServer, IGenericClient nandinaFhirServer,  Date reportDate) {
		this.targetFhirServer = targetFhirServer;
		this.nandinaFhirServer = nandinaFhirServer;
		ListResource encList = getEncounterListForDate(nandinaFhirServer, reportDate);
		init(encList);
	}
	
	private ListResource getEncounterListForDate(IGenericClient fhirServer, Date reportDate) {
		ListResource encounterList = null;
		Bundle bundle = this.rawSearch(fhirServer, "List?code=http://lantanagroup.com/fhir/us/nandina/CodeSystem/NandinaListType|ActiveEncountersForDay&date=" + sdf.format(reportDate));
		if (bundle.getTotal() > 1) {
			logger.debug("Multiple Nandina encounter lists found on same date. Only using first returned");
		}
		validationSupport = (IValidationSupport) fhirServer.getFhirContext().getValidationSupport();
		fpe = new FHIRPathEngine(new HapiWorkerContext(fhirServer.getFhirContext(), validationSupport));
		if (bundle.hasEntry() && bundle.getEntryFirstRep().hasResource()) {
			encounterList = (ListResource)bundle.getEntryFirstRep().getResource();
		}
		
		return encounterList;
	}

	private void init(ListResource encList) {

		xmlParser = targetFhirServer.getFhirContext().newXmlParser();
		loadEncounterMap(encList);
		loadPatientMaps();
		loadPatientData();
	}

	public void loadPatientData() {
		patientData = new ArrayList<PatientData>();
		for (String key : this.getPatientMap().keySet()) {
			PatientData pd;
			try {
				Patient p = this.getPatientMap().get(key);
				pd = new PatientData(this, p);
				patientData.add(pd);
			} catch (Exception e) {
				logger.info("Error loading data for " + key, e);
			} 
		}
	}

	public void loadEncounterMap(ListResource encList) {
		List<ListEntryComponent> entries = encList.getEntry();
		for (ListEntryComponent entry : entries) {
			
			List<Identifier> encIds = getIdentifiers(entry.getItem());
			for (Identifier encId : encIds) {
				Bundle search = getEncounter(encId);
				List<BundleEntryComponent> bEntries = search.getEntry();
				for (BundleEntryComponent bEntry : bEntries) {
					Encounter retrievedEnc = (Encounter)bEntry.getResource();
					this.encounterMap.put(retrievedEnc.getId(), retrievedEnc);
				}
			}
		}
	}

	private void loadPatientMaps(){
		for (String key: this.encounterMap.keySet()) {
			Encounter enc = this.encounterMap.get(key);
			String subjectRef = enc.getSubject().getReference();
			if (subjectRef.startsWith("Patient/")) {
				Patient p = targetFhirServer.read().resource(Patient.class).withId(subjectRef).execute();
				this.patientMap.put(p.getId(), p);
				this.patientEncounterMap.put(p, enc);
			} else {
				// It must be a Group, but Group can contain non-Patient resources, so deal with it later
				throw new RuntimeException("Encounter.subject of type Group not yet supported");
			}
		}
	}
	


	/**
	 * @return a Map of Encounter.id to Encounter
	 */
	public Map<String,Encounter> getEncounterMap() {
		return encounterMap;
	}

	/**
	 * @return a Map of Patient.id to Patient
	 */
	public Map<String,Patient> getPatientMap() {
		return patientMap;
	}
	
	public Map<Patient, Encounter> getPatientEncounterMap() {
		return patientEncounterMap;
	}
	
	public Bundle getEncounter(Identifier encId) {
		return rawSearch("Encounter?identifier=" + encId.getSystem() + "|" + encId.getValue()); 
	}
	
	public Bundle rawSearch(String query) {
        return rawSearch(targetFhirServer, query);
	}
	
	public Bundle rawSearch(IGenericClient fhirServer, String query) {
        try {
        	logger.debug("Executing query: " + query);
        	if (fhirServer == null) logger.debug("Client is null");
            Bundle bundle = targetFhirServer.search()
                    .byUrl(query)
                    .returnBundle(Bundle.class)
                    .execute();


            return bundle;
        } catch (Exception ex) {
            this.logger.error("Could not retrieve data for "  + this.getClass().getName() +  ": " + ex.getMessage(), ex);
        }
        return null;
	}
	
	

	private List<Identifier> getIdentifiers(Reference item) {
		List<Identifier> identifiers;
		if(item.hasReference()) {
			BaseResource res = (BaseResource) item.getResource();
			String type = item.getType();
			switch(type) {
				case "Encounter":
					identifiers = ((Encounter)res).getIdentifier();
				case "Patient":
					identifiers = ((Patient)res).getIdentifier();
				default:
					throw new RuntimeException("Update code to get identifiers for " + type);
			}
		} else {
			identifiers = new ArrayList<Identifier>();
			identifiers.add(item.getIdentifier());
		}
		return identifiers;
	}

	public List<PatientData> getPatientData() {
		return patientData;
	}

	



}
