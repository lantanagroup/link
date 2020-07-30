package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.PreviousDayAdmissionSuspectedCovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.Date;
import java.util.List;

public class PreviousDayAdmissionSuspectedCovidReport extends Report {
    public PreviousDayAdmissionSuspectedCovidReport(EncounterScoop scoop, List<Filter> filters, Date reportDate) {
        super(scoop, addFilters(filters, reportDate));
    }

    private static List<Filter> addFilters(List<Filter> filters, Date reportDate) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter admissionFilter = new PreviousDayAdmissionSuspectedCovidFilter(reportDate);
        filters.add(admissionFilter);
        return filters;
    }
}

