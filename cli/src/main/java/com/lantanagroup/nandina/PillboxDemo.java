package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.query.report.HospitalizedReport;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;

import java.text.SimpleDateFormat;

public class PillboxDemo {

  public static void main(String[] args) {
    try {
      FhirContext ctx = FhirContext.forR4();
      // Same FHIR server for nandina and EHR for demo purposes
      IGenericClient ehrFhirServer = ctx.newRestfulGenericClient("https://nandina-fhir.lantanagroup.com/fhir");
      IGenericClient nandinaFhirServer = ctx.newRestfulGenericClient("https://nandina-fhir.lantanagroup.com/fhir");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      // The Scoop will look on the nandinaFhirServer for a NandinaEncounterList where List.date is June 6, 2020, then start pulling Encounter data from the EHR with the encounter ids in the List
      EncounterScoop scoop = new EncounterScoop(ehrFhirServer, nandinaFhirServer, sdf.parse("2020-06-09"));
      HospitalizedReport hr = new HospitalizedReport(scoop, ctx);
      System.out.println("Patients hospitalized with Covid on report date: " + hr.getReportCount());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
