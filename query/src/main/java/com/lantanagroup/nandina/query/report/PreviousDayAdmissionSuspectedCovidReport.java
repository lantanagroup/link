package com.lantanagroup.nandina.query.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.filter.CovidFilter;
import com.lantanagroup.nandina.query.filter.Filter;
import com.lantanagroup.nandina.query.filter.PreviousDayAdmissionSuspectedCovidFilter;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;

import java.util.Date;
import java.util.List;

public class PreviousDayAdmissionSuspectedCovidReport extends Report {
    public PreviousDayAdmissionSuspectedCovidReport(EncounterScoop scoop, List<Filter> filters, Date reportDate, FhirContext ctx) {
        super(scoop, addFilters(filters, reportDate), ctx);
    }

    private static List<Filter> addFilters(List<Filter> filters, Date reportDate) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter admissionFilter = new PreviousDayAdmissionSuspectedCovidFilter(reportDate);
        filters.add(admissionFilter);
        return filters;
    }
}

