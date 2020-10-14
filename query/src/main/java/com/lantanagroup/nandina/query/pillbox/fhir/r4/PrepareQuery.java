package com.lantanagroup.nandina.query.pillbox.fhir.r4;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.EncounterScoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PrepareQuery extends BasePrepareQuery {
  private static final Logger logger = LoggerFactory.getLogger(PrepareQuery.class);

  @Override
  public void execute() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String reportDateValue = this.getCriteria().get("reportDate");
    Date reportDate;

    try {
      reportDate = sdf.parse(reportDateValue);
    } catch (Exception ex) {
      logger.error("Cannot prepare query wpithout a valid report date");
      return;
    }

    IGenericClient fhirQueryClient = (IGenericClient) this.getContextData("fhirQueryClient");
    IGenericClient fhirStoreClient = (IGenericClient) this.getContextData("fhirStoreClient");

    EncounterScoop encounterScoop = new EncounterScoop(fhirQueryClient, fhirStoreClient, reportDate);

    this.addContextData("scoopData", encounterScoop);
    this.addContextData("fhirContext", fhirQueryClient.getFhirContext());
  }
}
