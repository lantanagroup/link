package com.lantanagroup.nandina.query.fhir.r4.cerner.scoop;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.query.fhir.r4.cerner.PatientData;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncounterScoop extends Scoop {

  protected static final Logger logger = LoggerFactory.getLogger(EncounterScoop.class);
  protected IParser xmlParser;
  protected IGenericClient targetFhirServer;
  protected IGenericClient nandinaFhirServer;
  protected HashMap<String, Encounter> encounterMap = new HashMap<String, Encounter>();
  protected Map<String, Patient> patientMap = new HashMap<String, Patient>();
  protected Map<Patient, Encounter> patientEncounterMap = new HashMap<Patient, Encounter>();
  protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  protected IValidationSupport validationSupport;
  protected FHIRPathEngine fpe;


  public EncounterScoop(IGenericClient targetFhirServer, IGenericClient nandinaFhirServer, ListResource encList) {
    this.targetFhirServer = targetFhirServer;
    this.nandinaFhirServer = nandinaFhirServer;
    init(encList);
  }

  public EncounterScoop(IGenericClient targetFhirServer, IGenericClient nandinaFhirServer, List<String> encounterIdList) {
    this.targetFhirServer = targetFhirServer;
    this.nandinaFhirServer = nandinaFhirServer;
    init(encounterIdList);
  }

  public EncounterScoop(IGenericClient targetFhirServer, IGenericClient nandinaFhirServer, Date reportDate) {
    this.targetFhirServer = targetFhirServer;
    this.nandinaFhirServer = nandinaFhirServer;
    ListResource encList = getEncounterListForDate(reportDate);
    init(encList);
  }

  private ListResource getEncounterListForDate(Date reportDate) {
    ListResource encounterList = null;
    String search = "List?code=" + encodeValue("http://lantanagroup.com/fhir/us/nandina/CodeSystem/NandinaListType|ActiveEncountersForDay") + "&date=" + sdf.format(reportDate);
    Bundle bundle = this.rawSearch(nandinaFhirServer, search);
    if (bundle == null) {
    	throw new RuntimeException("Search returned null: " + search);
    } else if (bundle.getTotal() > 1) {
      logger.debug("Multiple Nandina encounter lists found on same date. Only using first returned");
    }
    validationSupport = (IValidationSupport) nandinaFhirServer.getFhirContext().getValidationSupport();
    fpe = new FHIRPathEngine(new HapiWorkerContext(nandinaFhirServer.getFhirContext(), validationSupport));
    if (bundle.hasEntry() && bundle.getEntryFirstRep().hasResource()) {
      encounterList = (ListResource) bundle.getEntryFirstRep().getResource();
    }
    reportDate = encounterList.getDate();
    return encounterList;
  }
  
  private String encodeValue(String value) {
	    try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

  private void init(List<String> encounterIdList) {
    loadEncounterMap(encounterIdList);
    loadPatientMaps();
    loadPatientData();
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
   logger.info("Initial patient count: " + patientData.size());
  }

  public void loadEncounterMap(List<String> encounterIdList) {
    for (String id : encounterIdList) {
      Encounter encounter = targetFhirServer.read().resource(Encounter.class).withId(id).execute();
      this.encounterMap.put(encounter.getId(), encounter);
    }
  }



  public void loadEncounterMap(ListResource encList) {
    List<ListEntryComponent> entries = encList.getEntry();
    for (ListEntryComponent entry : entries) {
      if (entry.getItem().hasReference()) {
          String encRef = entry.getItem().getReference();
    	  Encounter retrievedEnc = targetFhirServer.read().resource(Encounter.class).withId(encRef).execute();
    	  this.encounterMap.put(retrievedEnc.getId(), retrievedEnc);
      } else if (entry.getItem().hasIdentifier()) {
          List<Identifier> encIds = getIdentifiers(entry.getItem());
          for (Identifier encId : encIds) {
            Bundle search = getEncounter(encId);
            getEncountersFromSearchBundle(search);
          }
      } else {
    	  throw new RuntimeException("List.entry missing reference or identifier.");
      }
    }
  }

private void getEncountersFromSearchBundle(Bundle search) {
	List<BundleEntryComponent> bEntries = search.getEntry();
	for (BundleEntryComponent bEntry : bEntries) {
	  Encounter retrievedEnc = (Encounter) bEntry.getResource();
	  this.encounterMap.put(retrievedEnc.getId(), retrievedEnc);
	}
}

  private void loadPatientMaps() {
    for (String key : this.encounterMap.keySet()) {
      Encounter enc = this.encounterMap.get(key);
      String subjectRef = enc.getSubject().getReference();
      if (subjectRef.startsWith("Patient/")) {
        Patient p = targetFhirServer.read().resource(Patient.class).withId(subjectRef).execute();
        this.patientMap.put(p.getIdElement().getIdPart(), p);
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
  public Map<String, Encounter> getEncounterMap() {
    return encounterMap;
  }

  /**
   * @return a Map of Patient.id to Patient
   */
  public Map<String, Patient> getPatientMap() {
    return patientMap;
  }

  public Map<Patient, Encounter> getPatientEncounterMap() {
    return patientEncounterMap;
  }

  public Bundle getEncounter(String encId) {
    return rawSearch("Encounter?_id=" + encId);
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
      this.logger.error("Could not retrieve data for " + this.getClass().getName() + ": " + ex.getMessage(), ex);
    }
    return null;
  }


  private List<Identifier> getIdentifiers(Reference item) {
    List<Identifier> identifiers;
    if (item.hasReference()) {
      BaseResource res = (BaseResource) item.getResource();
      String type = item.getType();
      switch (type) {
        case "Encounter":
          identifiers = ((Encounter) res).getIdentifier();
        case "Patient":
          identifiers = ((Patient) res).getIdentifier();
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
