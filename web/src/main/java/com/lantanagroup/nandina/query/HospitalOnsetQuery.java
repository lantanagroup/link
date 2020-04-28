package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HospitalOnsetQuery implements IQueryCountExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HospitalOnsetQuery.class);

    @Override
    public Integer execute(IConfig config, IGenericClient fhirClient, String reportDate, String overflowLocations) {
        if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
            this.logger.error("Covid codes have not been specified in configuration. Cannot execute query.");
            return null;
        }

        /*
        String encounterDateStart = Helper.getFhirDate(LocalDateTime.now().minusDays(14));

        try {
            String url = String.format("Patient?_summary=true&_has:Condition:patient:code=%s&_has:Encounter:patient:class=IMP&_has:Encounter:patient:status=in-progress&_has:Encounter:patient:date=le%s",
                    Config.getInstance().getTerminologyCovidCodes(),
                    encounterDateStart);
            Bundle hospitalOnsetBundle = fhirClient.search()
                    .byUrl(url)
                    .returnBundle(Bundle.class)
                    .execute();
            return hospitalOnsetBundle.getTotal();
        } catch (Exception ex) {
            this.logger.error("Could not retrieve ED/overflow count: " + ex.getMessage(), ex);
        }
         */

        return null;
    }
}
