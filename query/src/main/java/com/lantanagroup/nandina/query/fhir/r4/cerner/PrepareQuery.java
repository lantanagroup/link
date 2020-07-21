package com.lantanagroup.nandina.query.fhir.r4.cerner;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class PrepareQuery extends BasePrepareQuery {
    @Autowired
    JsonProperties jsonProperties;

    @Override
    public void execute() {
        FhirContext ctx = FhirContext.forR4();
        IGenericClient ehrFhirServer = ctx.newRestfulGenericClient("https://nandina-fhir.lantanagroup.com/fhir");
        IGenericClient nandinaFhirServer = ctx.newRestfulGenericClient("https://nandina-fhir.lantanagroup.com/fhir");

        List<String> idList = new ArrayList<>();

        //TODO add this on back that has bad data...need to handle this with proper error handling
        //        idList.add("CER97953896");
        idList.add("CER97953897");
        idList.add("CER97953899");
        idList.add("CER97953898");
        idList.add("CER97733442");
        EncounterScoop encounterScoop = new EncounterScoop(ehrFhirServer, nandinaFhirServer, idList);

        this.addContextData("scoopData", encounterScoop);

        // TODO: Move core data used in HospitalizedQuery to here

        // Update HospitalizedQuery to use data stored in context, rather than storing HospitalizedQuery instance in context
    }
}