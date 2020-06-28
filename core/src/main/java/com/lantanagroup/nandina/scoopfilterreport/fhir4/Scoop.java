package com.lantanagroup.nandina.scoopfilterreport.fhir4;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Scoop {

	protected static final Logger logger = LoggerFactory.getLogger(Scoop.class);
	protected FhirContext ctx = FhirContext.forR4();
	protected IParser xmlParser = ctx.newXmlParser();
	protected IGenericClient client;
	protected HashMap<String,Encounter> encounterMap = new HashMap<String,Encounter>(); 
	protected Map<String,Patient> patientMap = new HashMap<String,Patient>(); 
	protected Map<Patient,Encounter> patientEncounterMap = new HashMap<Patient,Encounter>();
	
	
	public Scoop (String fhirBaseUrl, ListResource encList) {
		client = ctx.newRestfulGenericClient(fhirBaseUrl);
		loadEncounterMap(encList);
		loadPatientMaps();
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
				Patient p = client.read().resource(Patient.class).withId(subjectRef).execute();
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
	
	public Scoop(String fhirServerBase) {
		client = ctx.newRestfulGenericClient(fhirServerBase);
	}
	
	public Bundle getEncounter(Identifier encId) {
		return rawSearch("Encounter?identifier=" + encId.getSystem() + "|" + encId.getValue()); 
	}
	
	public Bundle rawSearch(String query) {
        try {
        	logger.debug("Executing query: " + query);
        	if (client == null) logger.debug("Client is null");
            Bundle bundle = client.search()
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

	



}
