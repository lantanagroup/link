package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalizedQuery extends AbstractQuery implements IQueryCountExecutor {
  public HospitalizedQuery(IConfig config, IGenericClient fhirClient) {
		super(config, fhirClient);
		// TODO Auto-generated constructor stub
	}


  @Override
  public Integer execute(String reportDate, String overflowLocations) {
    if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
      this.logger.error("Covid codes have not been specified in configuration. Cannot execute query.");
      return null;
    }

    
    /*
     * 
     * Enter the number of patients hospitalized in an inpatient bed at the time the data is collected who have suspected or confirmed COVID-19. This includes the patients with laboratory-confirmed or clinically diagnosed COVID-19.  
     * Confirmed: A patient with a laboratory confirmed COVID-19 diagnosis 
     * Suspected: A patient without a laboratory confirmed COVID-19 diagnosis who, in accordance with CDC’s Interim Public Health Guidance for Evaluating Persons Under Investigation (PUIs), has signs and symptoms compatible with COVID-19 (most patients with confirmed COVID-19 have developed fever and/or symptoms of acute respiratory illness, such as cough, shortness of breath or myalgia/fatigue).   
     */
    try {
      String url = String.format("Patient?_summary=true&_active=true&_has:Condition:patient:code=%s", config.getTerminologyCovidCodes());
      Bundle hospitalizedBundle = fhirClient.search()
              .byUrl(url)
              .returnBundle(Bundle.class)
              .execute();
      return hospitalizedBundle.getTotal();
    } catch (Exception ex) {
      this.logger.error("Could not retrieve hospitalized count: " + ex.getMessage(), ex);
    }

    return null;
  }
}
