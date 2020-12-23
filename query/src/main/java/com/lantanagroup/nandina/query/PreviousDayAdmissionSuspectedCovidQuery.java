package com.lantanagroup.nandina.query;

import com.lantanagroup.nandina.query.filter.Filter;
import com.lantanagroup.nandina.query.report.PreviousDayAdmissionSuspectedCovidReport;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreviousDayAdmissionSuspectedCovidQuery extends BaseQuery {
    @Override
    public Integer execute() {
        if (!this.criteria.containsKey("reportDate") && !this.criteria.containsKey("overflowLocations")) {
            return null;
        }

        EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
        List<Filter> filters = new ArrayList<Filter>();
        PreviousDayAdmissionSuspectedCovidReport onsetReport = new PreviousDayAdmissionSuspectedCovidReport(encounterScoop, filters, java.sql.Date.valueOf(LocalDate.parse(this.criteria.get("reportDate"))), this.fhirClient.getFhirContext());
        this.addContextData("previousDaySuspectedCovidAdmitted", onsetReport.getReportCount());

        return onsetReport.getReportCount();
    }

    @Override
    protected Map<String, Resource> queryForData() {
        return null;
    }
}
