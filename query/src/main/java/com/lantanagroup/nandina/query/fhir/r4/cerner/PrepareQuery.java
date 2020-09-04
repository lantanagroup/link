package com.lantanagroup.nandina.query.fhir.r4.cerner;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrepareQuery extends BasePrepareQuery {
    private static final Logger logger = LoggerFactory.getLogger(PrepareQuery.class);

    @Autowired
    JsonProperties jsonProperties;

    private List<String> getEncounterIds() {
        List<String> ids = new ArrayList<>();

        //TODO add this on back that has bad data...need to handle this with proper error handling
        //        idList.add("CER97953896");
        ids.add("CER97953897");
        ids.add("CER97953899");
        ids.add("CER97953898");
        ids.add("CER97733442");
        /*
        idList.add("CER97953899a");
        idList.add("CER97953899b");
        idList.add("CER97953899c");
        idList.add("CER97953899d");
        idList.add("CER97953899e");
        idList.add("CER97953899f");
        idList.add("CER97953899g");
        idList.add("CER97953899h");
         */

        return ids;
    }

    @Override
    public void execute() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String reportDateValue = this.getCriteria().get("reportDate");
        Date reportDate;

        try {
            reportDate = sdf.parse(reportDateValue);
        } catch (Exception ex) {
            logger.error("Cannot prepare query without a valid report date");
            return;
        }

        //FhirContext ctx = FhirContext.forR4();
        IGenericClient fhirQueryClient = (IGenericClient) this.getContextData("fhirQueryClient");
        IGenericClient fhirStoreClient = (IGenericClient) this.getContextData("fhirStoreClient");
        List<String> ids = this.getEncounterIds();

        EncounterScoop encounterScoop = new EncounterScoop(fhirQueryClient, fhirStoreClient, reportDate);

        this.addContextData("scoopData", encounterScoop);

        // TODO: Move core data used in HospitalizedQuery to here

        // Update HospitalizedQuery to use data stored in context, rather than storing HospitalizedQuery instance in context
    }
}