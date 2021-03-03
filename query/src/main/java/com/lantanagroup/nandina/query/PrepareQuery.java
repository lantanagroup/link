package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class PrepareQuery extends BasePrepareQuery {
    private static final Logger logger = LoggerFactory.getLogger(PrepareQuery.class);

    @Autowired
    NandinaConfig nandinaConfig;

    @Override
    public void execute() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String reportDateValue = this.getCriteria().get("reportDate");
        Date reportDate;

        try {
            reportDate = sdf.parse(reportDateValue);
        } catch (Exception ex) {
            logger.error("Cannot prepare query without a valid report date");
            return;
        }

        IGenericClient fhirQueryClient = (IGenericClient) this.getContextData("fhirQueryClient");
        IGenericClient fhirStoreClient = (IGenericClient) this.getContextData("fhirStoreClient");

        EncounterScoop encounterScoop = new EncounterScoop(fhirQueryClient, fhirStoreClient, reportDate);

        this.addContextData("scoopData", encounterScoop);
        this.addContextData("fhirContext", fhirQueryClient.getFhirContext());

        // TODO: Move core data used in HospitalizedQuery to here

        // Update HospitalizedQuery to use data stored in context, rather than storing HospitalizedQuery instance in context
    }
}